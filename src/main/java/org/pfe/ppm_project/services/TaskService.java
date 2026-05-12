package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.entities.Project;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.entities.TaskAssignment;
import org.pfe.ppm_project.entities.TaskDependency;
import org.pfe.ppm_project.enums.AuditAction;
import org.pfe.ppm_project.enums.DependencyType;
import org.pfe.ppm_project.enums.TaskStatus;
import org.pfe.ppm_project.exception.InvalidOperationException;
import org.pfe.ppm_project.exception.ResourceNotFoundException;
import org.pfe.ppm_project.repositories.ProjectRepository;
import org.pfe.ppm_project.repositories.TaskAssignmentRepository;
import org.pfe.ppm_project.repositories.TaskDependencyRepository;
import org.pfe.ppm_project.repositories.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Service metier pour la gestion des taches — version enterprise.
 *
 * NOUVELLES RESPONSABILITES (upgrade PPM) :
 *   1. Validation integrite hierarchie parentale (cycle, cross-project, profondeur max)
 *   2. Propagation combinee vers le parent : progress + status + workHours + dates
 *   3. Blocage de suppression si enfants actifs present
 *   4. Endpoint operationnel restreint pour DEV/QA/DEVOPS
 *
 * ALGORITHME DE PROPAGATION VERS LE PARENT (propagateToParent) :
 *   - Progress   : moyenne ponderee par workHours (identique Microsoft Project)
 *   - Status     : derive du statut des enfants actifs (DONE/BLOCKED/IN_PROGRESS/NOT_STARTED)
 *   - WorkHours  : somme des workHours des enfants actifs
 *   - Dates      : startDate = min(enfants), endDate = max(enfants)
 *   Recursion jusqu'a la racine de l'arbre WBS.
 *
 * PROTECTION ANTI-CYCLE (wouldCreateParentCycle) :
 *   BFS remontant l'arbre parentTask depuis le candidat parent.
 *   Si on croise l'ID de la tache en cours, cycle detecte → exception 400.
 *
 * PROFONDEUR MAX : MAX_HIERARCHY_DEPTH = 10 niveaux.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TaskService implements ITaskService {

    /** Profondeur maximale de la hierarchie WBS autorisee. */
    private static final int MAX_HIERARCHY_DEPTH = 10;

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ICalendarService calendarService;
    private final IAuditLogService auditLogService;
    private final TaskDependencyRepository dependencyRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    // ═══════════════════════════════════════════════════════════════
    // CRUD
    // ═══════════════════════════════════════════════════════════════

    @Override
    public Task createTask(Task task) {
        validateParentConstraints(task);   // Phase 1 : integrity checks
        computeEndDateFromCalendar(task);
        Task saved = taskRepository.save(task);

        Long projectId = saved.getProject() != null ? saved.getProject().getId() : null;
        auditLogService.log(
                AuditAction.CREATE, "TASK", saved.getId(), saved.getName(),
                "Task created in WBS position " + saved.getWbsNumber(),
                projectId, null
        );
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> getTasksByProject(Long projectId) {
        return taskRepository.findByProjectIdOrderBySortOrderAsc(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    @Override
    public Task updateTask(Long id, Task updatedTask) {
        return taskRepository.findById(id)
                .map(task -> {
                    TaskStatus oldStatus = task.getStatus();
                    LocalDate oldEndDate  = task.getEndDate();

                    // Phase 1 : valider la contrainte parent AVANT de l'appliquer
                    // On set temporairement le parentTask pour que validateParentConstraints
                    // ait acces a l'ID de la tache (task.id est connu ici).
                    Task parentCandidate = updatedTask.getParentTask();
                    task.setParentTask(parentCandidate); // temporaire pour la validation
                    validateParentConstraints(task);

                    task.setName(updatedTask.getName());
                    task.setDescription(updatedTask.getDescription());
                    task.setProject(updatedTask.getProject());
                    task.setDurationDays(updatedTask.getDurationDays());
                    task.setStartDate(updatedTask.getStartDate());
                    task.setEndDate(updatedTask.getEndDate());
                    task.setStatus(updatedTask.getStatus());
                    task.setProgress(updatedTask.getProgress());
                    task.setActive(updatedTask.isActive());
                    task.setWbsNumber(updatedTask.getWbsNumber());
                    task.setMode(updatedTask.getMode());
                    task.setWorkHours(updatedTask.getWorkHours());
                    task.setBaselineDurationDays(updatedTask.getBaselineDurationDays());
                    task.setBaselineStartDate(updatedTask.getBaselineStartDate());
                    task.setBaselineEndDate(updatedTask.getBaselineEndDate());
                    task.setActualWorkHours(updatedTask.getActualWorkHours());
                    task.setCalendarName(updatedTask.getCalendarName());
                    task.setSortOrder(updatedTask.getSortOrder());

                    computeEndDateFromCalendar(task);
                    Task saved = taskRepository.save(task);

                    Long projectId = saved.getProject() != null ? saved.getProject().getId() : null;
                    if (updatedTask.getStatus() != null && oldStatus != updatedTask.getStatus()) {
                        auditLogService.log(
                                AuditAction.STATUS_CHANGE, "TASK", saved.getId(), saved.getName(),
                                "Status: " + oldStatus + " -> " + updatedTask.getStatus()
                                        + " | Progress: " + saved.getProgress() + "%",
                                projectId, null
                        );
                    } else {
                        auditLogService.log(
                                AuditAction.UPDATE, "TASK", saved.getId(), saved.getName(),
                                "Task updated | Progress: " + saved.getProgress() + "%",
                                projectId, null
                        );
                    }

                    // Propagation combinee : progress + status + workHours + dates
                    propagateToParent(saved.getParentTask());

                    // Cascade FS si la date de fin a change
                    if (!Objects.equals(oldEndDate, saved.getEndDate())) {
                        cascadeFinishStartDependencies(saved, new HashSet<>());
                    }

                    return saved;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    /**
     * Mise a jour rapide du statut uniquement — Quick Actions My Tasks.
     * Ajuste automatiquement le progres selon le statut.
     * Propage ensuite progress + status + workHours + dates vers les parents.
     */
    @Override
    public Task updateTaskStatus(Long id, TaskStatus newStatus) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);

        if (newStatus == TaskStatus.DONE) {
            task.setProgress(100);
        } else if (oldStatus == TaskStatus.DONE && newStatus != TaskStatus.DONE) {
            task.setProgress(75);
        } else if (newStatus == TaskStatus.NOT_STARTED && task.getProgress() == null) {
            task.setProgress(0);
        }

        Task saved = taskRepository.save(task);

        Long projectId = saved.getProject() != null ? saved.getProject().getId() : null;
        auditLogService.log(
                AuditAction.STATUS_CHANGE, "TASK", saved.getId(), saved.getName(),
                "Quick status update: " + oldStatus + " -> " + newStatus
                        + " | Progress: " + saved.getProgress() + "%",
                projectId, null
        );

        propagateToParent(saved.getParentTask());
        return saved;
    }

    /**
     * Mise a jour operationnelle restreinte — endpoint dedie DEV/QA/DEVOPS.
     * Seuls status, progress et actualWorkHours sont modifiables.
     * Propage la hierarchie WBS apres modification.
     */
    @Override
    public Task updateTaskOperational(Long id, TaskStatus status, Integer progress, Double actualWorkHours) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        TaskStatus oldStatus = task.getStatus();

        if (status != null)           task.setStatus(status);
        if (progress != null)         task.setProgress(Math.max(0, Math.min(100, progress)));
        if (actualWorkHours != null)  task.setActualWorkHours(actualWorkHours);

        Task saved = taskRepository.save(task);

        Long projectId = saved.getProject() != null ? saved.getProject().getId() : null;
        auditLogService.log(
                AuditAction.UPDATE, "TASK", saved.getId(), saved.getName(),
                "Operational update: status=" + oldStatus + "→" + status + ", progress=" + progress
                        + "%, actualWork=" + actualWorkHours + "h",
                projectId, null
        );

        propagateToParent(saved.getParentTask());
        return saved;
    }

    @Override
    public void deactivateTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        task.setActive(false);
        taskRepository.save(task);

        Long projectId = task.getProject() != null ? task.getProject().getId() : null;
        auditLogService.log(
                AuditAction.DELETE, "TASK", task.getId(), task.getName(),
                "Task deactivated (soft delete)", projectId, null
        );
    }

    @Override
    public void setTaskActive(Long id, boolean active) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        task.setActive(active);
        taskRepository.save(task);

        Long projectId = task.getProject() != null ? task.getProject().getId() : null;
        auditLogService.log(
                AuditAction.UPDATE, "TASK", id, task.getName(),
                active ? "Task reactivated" : "Task deactivated", projectId, null
        );
    }

    /**
     * Suppression physique d'une tache.
     *
     * PHASE 6 — Blocage si la tache a des enfants actifs.
     * Un message clair est retourne : l'utilisateur doit d'abord supprimer
     * ou reassigner les sous-taches avant de supprimer la tache parente.
     * Cela evite de creer des taches racines orphelines sans avertissement.
     */
    @Override
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        // Phase 6 : bloquer si la tache a des enfants actifs
        long activeChildCount = taskRepository.countByParentTaskIdAndActiveTrue(id);
        if (activeChildCount > 0) {
            throw new InvalidOperationException(
                    "Cannot delete task '" + task.getName() + "': it has " + activeChildCount
                    + " active subtask(s). Delete or reassign subtasks first.");
        }

        // Detacher les enfants inactifs restants pour eviter la violation de FK
        List<Task> remainingChildren = taskRepository.findByParentTaskId(id);
        for (Task child : remainingChildren) {
            child.setParentTask(null);
            taskRepository.save(child);
        }

        dependencyRepository.deleteByTaskIds(List.of(id));

        List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(id);
        taskAssignmentRepository.deleteAll(assignments);

        Long projectId = task.getProject() != null ? task.getProject().getId() : null;
        String taskName = task.getName();

        taskRepository.delete(task);

        auditLogService.log(
                AuditAction.DELETE, "TASK", id, taskName,
                "Task permanently deleted", projectId, null
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // PHASE 1 — VALIDATION INTEGRITE HIERARCHIE PARENTALE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Valide toutes les contraintes liees a la tache parente avant sauvegarde :
     *   1. Auto-reference : une tache ne peut pas etre son propre parent.
     *   2. Cycle : le parent candidat ne doit pas etre un descendant de cette tache.
     *   3. Projet croise : parent et enfant doivent appartenir au meme projet.
     *   4. Profondeur max : l'arbre ne peut pas depasser MAX_HIERARCHY_DEPTH niveaux.
     *
     * Methode idempotente — aucun effet de bord.
     */
    private void validateParentConstraints(Task task) {
        if (task.getParentTask() == null || task.getParentTask().getId() == null) return;

        Long parentId = task.getParentTask().getId();
        Long taskId   = task.getId(); // null a la creation

        // 1. Auto-reference
        if (taskId != null && taskId.equals(parentId)) {
            throw new InvalidOperationException("A task cannot be its own parent");
        }

        // 2. Detection de cycle via BFS remontant l'arbre parent
        if (taskId != null && wouldCreateParentCycle(parentId, taskId)) {
            throw new InvalidOperationException(
                    "Circular parent hierarchy detected: setting this parent would create a cycle in the WBS tree");
        }

        // 3. Charger le parent pour les verifications suivantes
        Task parent = taskRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent task", parentId));

        // 4. Validation cross-project
        Long taskProjectId   = task.getProject()   != null ? task.getProject().getId()   : null;
        Long parentProjectId = parent.getProject() != null ? parent.getProject().getId() : null;
        if (taskProjectId != null && parentProjectId != null && !taskProjectId.equals(parentProjectId)) {
            throw new InvalidOperationException(
                    "Parent task must belong to the same project");
        }

        // 5. Profondeur maximale
        int depth = computeAncestorDepth(parentId);
        if (depth >= MAX_HIERARCHY_DEPTH) {
            throw new InvalidOperationException(
                    "Maximum WBS hierarchy depth of " + MAX_HIERARCHY_DEPTH + " levels exceeded");
        }
    }

    /**
     * Detecte si l'ajout de parentId comme parent de taskId creerait un cycle.
     *
     * ALGORITHME BFS remontant l'arbre depuis parentId :
     *   A chaque iteration, on remonte vers le grand-parent.
     *   Si on croise taskId, le cycle existerait — on retourne true.
     *
     * Exemple : A → parent B → parent A (cycle A-B)
     *   On veut set parent de B = A.
     *   BFS depuis A : parent(A) = B → B == taskId(B) → CYCLE DETECTE.
     *
     * O(D) ou D = profondeur de l'arbre.
     */
    private boolean wouldCreateParentCycle(Long parentId, Long taskId) {
        Set<Long> visited = new HashSet<>();
        Long current = parentId;

        while (current != null) {
            if (current.equals(taskId)) return true;
            if (!visited.add(current))  break; // protection cycle residuel

            Task t = taskRepository.findById(current).orElse(null);
            if (t == null) break;

            current = (t.getParentTask() != null) ? t.getParentTask().getId() : null;
        }
        return false;
    }

    /**
     * Calcule le nombre de niveaux d'ancetres de la tache donnee.
     * Utilise pour valider la profondeur maximale.
     */
    private int computeAncestorDepth(Long taskId) {
        int depth   = 0;
        Long current = taskId;
        Set<Long> visited = new HashSet<>();

        while (current != null && depth <= MAX_HIERARCHY_DEPTH) {
            if (!visited.add(current)) break;
            Task t = taskRepository.findById(current).orElse(null);
            if (t == null) break;
            current = (t.getParentTask() != null) ? t.getParentTask().getId() : null;
            if (current != null) depth++;
        }
        return depth;
    }

    // ═══════════════════════════════════════════════════════════════
    // PHASE 3 — PROPAGATION COMBINEE VERS LE PARENT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Propage en une seule passe les quatre metriques vers la tache parente :
     *   1. Progress   : moyenne ponderee par workHours (formule Microsoft Project)
     *   2. Status     : derive du statut des enfants actifs
     *   3. WorkHours  : somme des workHours des enfants actifs
     *   4. Dates      : startDate = min, endDate = max des enfants actifs
     *
     * Recursive jusqu'a la racine. Protection : si la tache parente n'existe
     * plus en base, la recursion s'arrete proprement.
     *
     * Remplace l'ancienne methode propagateProgressToParent().
     */
    private void propagateToParent(Task parent) {
        if (parent == null || parent.getId() == null) return;

        Task parentTask = taskRepository.findById(parent.getId()).orElse(null);
        if (parentTask == null) return;

        List<Task> children       = taskRepository.findByParentTaskId(parentTask.getId());
        List<Task> activeChildren = children.stream().filter(Task::isActive).toList();

        if (activeChildren.isEmpty()) return;

        // 1. Progress (weighted average by workHours)
        double totalWeight = activeChildren.stream()
                .mapToDouble(t -> t.getWorkHours() != null ? t.getWorkHours() : 1.0)
                .sum();
        double weightedProgress = activeChildren.stream()
                .mapToDouble(t -> {
                    double w = t.getWorkHours() != null ? t.getWorkHours() : 1.0;
                    double p = t.getProgress() != null ? t.getProgress()   : 0.0;
                    return w * p;
                })
                .sum();
        int computedProgress = totalWeight > 0
                ? (int) Math.round(weightedProgress / totalWeight)
                : 0;
        parentTask.setProgress(Math.max(0, Math.min(100, computedProgress)));

        // 2. Status (derived from children)
        parentTask.setStatus(deriveParentStatus(activeChildren));

        // 3. WorkHours (sum of children)
        double totalWorkHours = activeChildren.stream()
                .mapToDouble(t -> t.getWorkHours() != null ? t.getWorkHours() : 0.0)
                .sum();
        if (totalWorkHours > 0) {
            parentTask.setWorkHours(totalWorkHours);
        }

        // 4. Dates (min start, max end)
        activeChildren.stream()
                .map(Task::getStartDate)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .ifPresent(parentTask::setStartDate);

        activeChildren.stream()
                .map(Task::getEndDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .ifPresent(parentTask::setEndDate);

        taskRepository.save(parentTask);

        // Recursion vers le grand-parent
        propagateToParent(parentTask.getParentTask());
    }

    /**
     * Derive le statut d'une tache parente depuis ses enfants actifs.
     *
     * Priorite :
     *   DONE       — tous les enfants sont DONE
     *   BLOCKED    — au moins un enfant est BLOCKED
     *   IN_PROGRESS — au moins un enfant est IN_PROGRESS
     *   NOT_STARTED — tous les enfants sont NOT_STARTED
     *   IN_PROGRESS — cas mixte residuel
     */
    private TaskStatus deriveParentStatus(List<Task> activeChildren) {
        boolean allDone       = activeChildren.stream().allMatch(t -> t.getStatus() == TaskStatus.DONE);
        if (allDone) return TaskStatus.DONE;

        boolean anyBlocked    = activeChildren.stream().anyMatch(t -> t.getStatus() == TaskStatus.BLOCKED);
        if (anyBlocked) return TaskStatus.BLOCKED;

        boolean anyInProgress = activeChildren.stream().anyMatch(t -> t.getStatus() == TaskStatus.IN_PROGRESS);
        if (anyInProgress) return TaskStatus.IN_PROGRESS;

        boolean allNotStarted = activeChildren.stream().allMatch(t -> t.getStatus() == TaskStatus.NOT_STARTED);
        if (allNotStarted) return TaskStatus.NOT_STARTED;

        return TaskStatus.IN_PROGRESS;
    }

    // ═══════════════════════════════════════════════════════════════
    // CALCUL DE LA DATE DE FIN PAR CALENDRIER
    // ═══════════════════════════════════════════════════════════════

    /**
     * Calcule la date de fin d'une tache a partir de sa date de debut et de sa duree,
     * en utilisant le calendrier de travail du projet (jours ouvres uniquement).
     * Ne recalcule que si endDate n'est pas deja definie.
     */
    private void computeEndDateFromCalendar(Task task) {
        if (task.getStartDate() == null || task.getDurationDays() == null) return;
        if (task.getEndDate() != null) return;

        Long calendarId = getCalendarIdForTask(task);
        int workDays = (int) Math.ceil(task.getDurationDays());
        if (workDays < 1) workDays = 1;

        LocalDate endDate = calendarService.addWorkingDays(calendarId, task.getStartDate(), workDays);
        task.setEndDate(endDate);
    }

    private Long getCalendarIdForTask(Task task) {
        if (task.getProject() != null && task.getProject().getId() != null) {
            Project project = projectRepository.findById(task.getProject().getId()).orElse(null);
            if (project != null && project.getCalendar() != null) {
                return project.getCalendar().getId();
            }
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════
    // CASCADE FS SUR LES DEPENDANCES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Propage en cascade le changement de date de fin vers les taches successeurs FS.
     * Seules les dependances Finish-to-Start sont cascadees automatiquement.
     */
    private void cascadeFinishStartDependencies(Task task, Set<Long> visited) {
        if (task.getEndDate() == null || task.getId() == null) return;
        if (!visited.add(task.getId())) return;

        Long calendarId = getCalendarIdForTask(task);

        List<TaskDependency> fsDeps = dependencyRepository.findByPredecessor_Id(task.getId())
                .stream()
                .filter(d -> d.getType() == DependencyType.FS)
                .toList();

        for (TaskDependency dep : fsDeps) {
            if (dep.getSuccessor() == null) continue;

            Task succ = taskRepository.findById(dep.getSuccessor().getId()).orElse(null);
            if (succ == null || !succ.isActive()) continue;

            LocalDate newStart    = calendarService.addWorkingDays(calendarId, task.getEndDate(), 1);
            LocalDate oldSuccEnd  = succ.getEndDate();

            succ.setStartDate(newStart);
            succ.setEndDate(null);
            computeEndDateFromCalendar(succ);
            taskRepository.save(succ);

            if (!Objects.equals(oldSuccEnd, succ.getEndDate())) {
                cascadeFinishStartDependencies(succ, visited);
            }
        }
    }
}
