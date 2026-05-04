package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pfe.ppm_project.dto.MyTaskDTO;
import org.pfe.ppm_project.enums.AuditAction;
import org.pfe.ppm_project.enums.NotificationType;
import org.pfe.ppm_project.exception.InvalidOperationException;
import org.pfe.ppm_project.exception.ResourceNotFoundException;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.entities.TaskAssignment;
import org.pfe.ppm_project.entities.User;
import org.pfe.ppm_project.repositories.TaskAssignmentRepository;
import org.pfe.ppm_project.repositories.TaskRepository;
import org.pfe.ppm_project.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service metier pour la gestion des affectations de ressources aux taches.
 * Controle que le total des heures assignees ne depasse pas la charge prevue
 * de la tache (workHours) et empeche les doublons d'affectation.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskAssignmentService implements ITaskAssignmentService {

    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final IAuditLogService auditLogService;
    private final INotificationService notificationService;

    // Affecte un utilisateur a une tache avec un nombre d'heures donne.
    // Verifie que l'affectation n'existe pas deja et que le total ne depasse pas la charge prevue.
    @Override
    public TaskAssignment assignUserToTask(TaskAssignment assignment) {
        // Validations des champs obligatoires
        if (assignment == null) throw new InvalidOperationException("assignment is required");
        if (assignment.getTask() == null || assignment.getTask().getId() == null)
            throw new InvalidOperationException("taskId is required");
        if (assignment.getUser() == null || assignment.getUser().getId() == null)
            throw new InvalidOperationException("userId is required");
        if (assignment.getAssignedHours() == null || assignment.getAssignedHours() < 0)
            throw new InvalidOperationException("assignedHours must be >= 0");

        Long taskId = assignment.getTask().getId();
        Long userId = assignment.getUser().getId();

        // Verification anti-doublon : un utilisateur actif ne peut etre affecte qu'une fois a une tache.
        // Note : on verifie uniquement les affectations ACTIVES pour permettre la reactivation.
        if (taskAssignmentRepository.existsActiveByTaskIdAndUserId(taskId, userId)) {
            throw new InvalidOperationException("This user is already assigned to this task");
        }

        // Chargement des entites depuis la base
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Verification que l'utilisateur est actif (un compte desactive ne peut pas etre affecte)
        if (!user.isActive()) {
            throw new InvalidOperationException("Cannot assign inactive user to task");
        }

        // Controle de capacite : le total des heures assignees ne doit pas depasser la charge prevue
        Double plannedWork = task.getWorkHours();
        if (plannedWork == null) throw new InvalidOperationException("Task workHours is null");

        int workloadLimit = (int) Math.ceil(plannedWork);

        int alreadyAssigned = taskAssignmentRepository.sumActiveAssignedHoursByTask(taskId);
        if (alreadyAssigned + assignment.getAssignedHours() > workloadLimit) {
            throw new InvalidOperationException(
                "Total assigned hours (" + (alreadyAssigned + assignment.getAssignedHours())
                + "h) exceed task planned work (" + workloadLimit + "h)"
            );
        }

        // Reactivation : si une affectation inactive existe deja pour ce couple (tache, utilisateur),
        // on la reactive plutot que d'inserer un nouvel enregistrement (la contrainte DB l'interdirait).
        TaskAssignment existing = taskAssignmentRepository
                .findByTaskIdAndUserId(taskId, userId)
                .orElse(null);

        TaskAssignment saved;
        if (existing != null) {
            // Reactivation de l'affectation desactivee
            existing.setActive(true);
            existing.setAssignedHours(assignment.getAssignedHours());
            existing.setTask(task);
            existing.setUser(user);
            saved = taskAssignmentRepository.save(existing);
        } else {
            assignment.setTask(task);
            assignment.setUser(user);
            assignment.setActive(true);
            saved = taskAssignmentRepository.save(assignment);
        }

        // Audit : enregistre l'affectation avec le contexte projet
        Long projectId = task.getProject() != null ? task.getProject().getId() : null;
        String projectName = task.getProject() != null ? task.getProject().getName() : null;
        auditLogService.log(
                AuditAction.ASSIGN, "ASSIGNMENT", saved.getId(),
                user.getFirstName() + " " + user.getLastName() + " -> " + task.getName(),
                user.getEmail() + " assigned to task '" + task.getName()
                        + "' (" + saved.getAssignedHours() + "h)",
                projectId, projectName
        );

        // Notification : informe l'utilisateur de sa nouvelle affectation.
        // ISOLATION : enrobage try-catch obligatoire — la notification est un effet de bord.
        // Si elle echoue (ex: charset MySQL, contrainte), l'affectation ne doit pas etre perdue.
        // Le message n'utilise que des caracteres ASCII pour eviter toute erreur de charset.
        try {
            notificationService.createNotification(
                    user.getId(),
                    NotificationType.TASK_ASSIGNED,
                    "New task assignment",
                    "You have been assigned to task '" + task.getName() + "'"
                            + (projectName != null ? " in project '" + projectName + "'" : "")
                            + " - " + saved.getAssignedHours() + "h allocated.",
                    "TASK",
                    task.getId()
            );
        } catch (Exception notifEx) {
            // La notification a echoue mais l'affectation est deja persistee — on continue.
            // On loggue en warn pour faciliter le diagnostic sans faire echouer le flux metier.
            log.warn("[ASSIGNMENT] Notification creation failed for userId={} taskId={}: {}",
                    user.getId(), task.getId(), notifEx.getMessage());
        }

        return saved;
    }

    // Retourne toutes les affectations d'une tache donnee
    @Override
    @Transactional(readOnly = true)
    public List<TaskAssignment> getAssignmentsByTask(Long taskId) {
        return taskAssignmentRepository.findByTaskIdWithRefs(taskId);
    }

    // Retourne toutes les affectations d'un utilisateur donne
    @Override
    @Transactional(readOnly = true)
    public List<TaskAssignment> getAssignmentsByUser(Long userId) {
        return taskAssignmentRepository.findByUserIdWithRefs(userId);
    }

    // Met a jour les heures assignees d'une affectation existante.
    // Verifie que le nouveau total ne depasse pas la charge prevue de la tache.
    @Override
    public TaskAssignment updateAssignedHours(Long assignmentId, Integer assignedHours) {
        if (assignedHours == null || assignedHours < 0)
            throw new InvalidOperationException("assignedHours must be >= 0");

        TaskAssignment existing = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        // Impossible de modifier une affectation desactivee
        if (!existing.isActive()) {
            throw new InvalidOperationException("Cannot update hours of an inactive assignment");
        }

        Long taskId = existing.getTask().getId();
        Double plannedWork = existing.getTask().getWorkHours();
        if (plannedWork == null) throw new InvalidOperationException("Task workHours is null");

        int workloadLimit = (int) Math.ceil(plannedWork);

        // Calcul du total sans l'affectation courante pour eviter de se compter deux fois
        int othersAssigned = taskAssignmentRepository.sumActiveAssignedHoursByTaskExcluding(taskId, assignmentId);
        if (othersAssigned + assignedHours > workloadLimit) {
            throw new InvalidOperationException("Total assigned hours exceed task planned work (" + workloadLimit + "h)");
        }

        existing.setAssignedHours(assignedHours);
        return taskAssignmentRepository.save(existing);
    }

    // Desactive une affectation (suppression logique)
    @Override
    public void deactivateAssignment(Long assignmentId) {
        TaskAssignment existing = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        existing.setActive(false);
        taskAssignmentRepository.save(existing);

        // Audit : enregistre le retrait d'affectation
        Task t = existing.getTask();
        User u = existing.getUser();
        Long projectId = t != null && t.getProject() != null ? t.getProject().getId() : null;
        auditLogService.log(
                AuditAction.UNASSIGN, "ASSIGNMENT", assignmentId,
                u != null ? u.getEmail() : "unknown",
                "Assignment removed" + (t != null ? " from task '" + t.getName() + "'" : ""),
                projectId, null
        );
    }

    /**
     * Retourne les taches assignees a l'utilisateur identifie par userId, enrichies avec
     * les informations de la tache et du projet parent.
     *
     * La requete utilise JOIN FETCH pour charger la tache et le projet en une seule requete SQL,
     * evitant ainsi le probleme N+1 queries.
     *
     * Filtres appliques (voir TaskAssignmentRepository.findActiveAssignmentsByUserId) :
     * - Affectation active (assignment.active = true)
     * - Tache active (task.active = true) — exclut les taches supprimees en douceur
     *
     * Le DTO MyTaskDTO est construit en memoire a partir des entites chargees.
     * Logique Microsoft PPM : "My Tasks" affiche uniquement ce qui est explicitement affecte
     * au collaborateur, avec le contexte projet pour la navigation.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MyTaskDTO> getMyTasks(Long userId) {
        return taskAssignmentRepository.findActiveAssignmentsByUserId(userId)
                .stream()
                .map(ta -> {
                    Task t = ta.getTask();
                    return new MyTaskDTO(
                            ta.getId(),
                            t.getId(),
                            t.getName(),
                            t.getStatus() != null ? t.getStatus().name() : "NOT_STARTED",
                            t.getProgress() != null ? t.getProgress() : 0,
                            t.getStartDate() != null ? t.getStartDate().toString() : null,
                            t.getEndDate() != null ? t.getEndDate().toString() : null,
                            t.getWbsNumber(),
                            ta.getAssignedHours(),
                            t.getProject().getId(),
                            t.getProject().getName(),
                            t.getDurationDays(),
                            t.getWorkHours()
                    );
                })
                .toList();
    }
}
