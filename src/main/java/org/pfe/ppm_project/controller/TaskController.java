package org.pfe.ppm_project.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.TaskDTO;
import org.pfe.ppm_project.dto.TaskOperationalUpdateDTO;
import org.pfe.ppm_project.entities.Project;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.enums.TaskStatus;
import org.pfe.ppm_project.exception.InvalidOperationException;
import org.pfe.ppm_project.mapper.TaskMapper;
import org.pfe.ppm_project.repositories.TaskAssignmentRepository;
import org.pfe.ppm_project.repositories.TaskRepository;
import org.pfe.ppm_project.services.ITaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller CRUD des taches — version enterprise upgrade.
 *
 * RBAC mis en conformite :
 *   POST   /api/tasks           — ADMIN / PMO / PM uniquement
 *   GET    /api/tasks/**        — tous sauf RH
 *   PUT    /api/tasks/{id}      — ADMIN / PMO / PM uniquement (restreint depuis cette version)
 *   PATCH  /api/tasks/{id}/operational — DEV / QA / DEVOPS (status + progress + actualWorkHours)
 *   PATCH  /api/tasks/{id}/status      — DEV / QA / DEVOPS sur taches assignees
 *   PATCH  /api/tasks/{id}/active      — ADMIN / PMO / PM
 *   DELETE /api/tasks/{id}             — ADMIN / PMO / PM (bloque si enfants actifs)
 *
 * Le nouveau endpoint PATCH /operational garantit que les membres techniques
 * ne peuvent jamais modifier les champs de planification via l'API, meme directement.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final ITaskService taskService;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskRepository taskRepository;

    // ─────────────────────────────────────────────────────────────
    // CREATION — ADMIN / PMO / PM seulement
    // ─────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')")
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody TaskDTO dto) {
        validateDates(dto);
        Task task = buildTaskFromDto(dto, true);
        Task created = taskService.createTask(task);
        boolean hasChildren = taskRepository.existsByParentTaskId(created.getId());
        return new ResponseEntity<>(TaskMapper.toDTO(created, null, hasChildren), HttpStatus.CREATED);
    }

    // ─────────────────────────────────────────────────────────────
    // LECTURE — tous sauf RH
    // ─────────────────────────────────────────────────────────────

    /**
     * Retourne toutes les taches d'un projet avec le flag hasChildren calculé en memoire (O(n)).
     * Aucune requete supplementaire : on derive les parents depuis la liste chargee.
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM','DEV','QA','DEVOPS')")
    public ResponseEntity<List<TaskDTO>> getByProject(@PathVariable Long projectId) {
        List<Task> tasks = taskService.getTasksByProject(projectId);

        // Calcule en memoire les IDs qui sont references comme parent par au moins un autre task
        Set<Long> parentIds = tasks.stream()
                .filter(t -> t.getParentTask() != null && t.getParentTask().getId() != null)
                .map(t -> t.getParentTask().getId())
                .collect(Collectors.toSet());

        List<TaskDTO> dtos = tasks.stream()
                .map(t -> TaskMapper.toDTO(t, null, parentIds.contains(t.getId())))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /** Retourne une tache par ID avec assignedUserIds et hasChildren. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM','DEV','QA','DEVOPS')")
    public ResponseEntity<TaskDTO> getById(@PathVariable Long id) {
        return taskService.getTaskById(id)
                .map(task -> {
                    List<Long> assignedUserIds = taskAssignmentRepository.findActiveUserIdsByTaskId(id);
                    boolean hasChildren = taskRepository.existsByParentTaskId(id);
                    return TaskMapper.toDTO(task, assignedUserIds, hasChildren);
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────────────────────────
    // MISE A JOUR COMPLETE — ADMIN / PMO / PM uniquement (PHASE 2)
    // ─────────────────────────────────────────────────────────────

    /**
     * Mise a jour complete d'une tache.
     * RESTREINTE a ADMIN / PMO / PM depuis cette version.
     * Les DEV / QA / DEVOPS doivent utiliser PATCH /operational.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')")
    public ResponseEntity<TaskDTO> update(@PathVariable Long id, @Valid @RequestBody TaskDTO dto) {
        validateDates(dto);
        Task updated = buildTaskFromDto(dto, false);
        Task saved = taskService.updateTask(id, updated);
        boolean hasChildren = taskRepository.existsByParentTaskId(saved.getId());
        return ResponseEntity.ok(TaskMapper.toDTO(saved, null, hasChildren));
    }

    // ─────────────────────────────────────────────────────────────
    // MISE A JOUR OPERATIONNELLE — DEV / QA / DEVOPS (PHASE 2)
    // ─────────────────────────────────────────────────────────────

    /**
     * Endpoint restreint pour les membres techniques (DEV / QA / DEVOPS).
     * Seuls status, progress et actualWorkHours peuvent etre modifies.
     *
     * RBAC :
     *   - ADMIN / PMO / PM : acces libre.
     *   - DEV / QA / DEVOPS : uniquement sur les taches auxquelles ils sont affectes.
     *
     * Le backend enforce ce contrat independamment du frontend.
     */
    @PatchMapping("/{id}/operational")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM','DEV','QA','DEVOPS')")
    public ResponseEntity<TaskDTO> updateOperational(
            @PathVariable Long id,
            @Valid @RequestBody TaskOperationalUpdateDTO dto) {

        enforceTeamMemberAssignment(id);

        Task saved = taskService.updateTaskOperational(id, dto.status(), dto.progress(), dto.actualWorkHours());
        boolean hasChildren = taskRepository.existsByParentTaskId(saved.getId());
        return ResponseEntity.ok(TaskMapper.toDTO(saved, null, hasChildren));
    }

    // ─────────────────────────────────────────────────────────────
    // MISE A JOUR RAPIDE DU STATUT — Quick Actions
    // ─────────────────────────────────────────────────────────────

    /**
     * Mise a jour rapide du statut uniquement.
     * RBAC identique au endpoint /operational.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM','DEV','QA','DEVOPS')")
    public ResponseEntity<TaskDTO> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {

        String rawStatus = payload.get("status");
        if (rawStatus == null || rawStatus.isBlank()) {
            throw new InvalidOperationException("Missing 'status' field in request body");
        }

        TaskStatus newStatus;
        try {
            newStatus = TaskStatus.valueOf(rawStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOperationException("Invalid status value: " + rawStatus
                    + ". Valid values: NOT_STARTED, IN_PROGRESS, DONE, BLOCKED");
        }

        enforceTeamMemberAssignment(id);

        Task updated = taskService.updateTaskStatus(id, newStatus);
        return ResponseEntity.ok(TaskMapper.toDTO(updated));
    }

    // ─────────────────────────────────────────────────────────────
    // ACTIVATION / DESACTIVATION — ADMIN / PMO / PM
    // ─────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')")
    public ResponseEntity<Void> setActive(@PathVariable Long id, @RequestParam boolean active) {
        taskService.setTaskActive(id, active);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────
    // SUPPRESSION — ADMIN / PMO / PM (bloquee si enfants actifs)
    // ─────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────
    // UTILITAIRES PRIVES
    // ─────────────────────────────────────────────────────────────

    /**
     * Pour les roles DEV / QA / DEVOPS : verifie que l'utilisateur courant
     * est bien affecte a la tache cible avant d'autoriser la modification.
     * Les managers (ADMIN / PMO / PM) ne sont pas soumis a cette verification.
     */
    private void enforceTeamMemberAssignment(Long taskId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(a -> {
                    String r = a.getAuthority();
                    return r.equals("ROLE_ADMIN") || r.equals("ROLE_PMO") || r.equals("ROLE_PM");
                });

        if (!isManager) {
            Long currentUserId = (Long) auth.getDetails();
            boolean assigned = taskAssignmentRepository.existsActiveByTaskIdAndUserId(taskId, currentUserId);
            if (!assigned) {
                throw new AccessDeniedException("You are not assigned to this task");
            }
        }
    }

    /** Construit une entite Task a partir du DTO. forceActive = true uniquement a la creation. */
    private Task buildTaskFromDto(TaskDTO dto, boolean forceActive) {
        return Task.builder()
                .name(dto.name())
                .description(dto.description())
                .project(Project.builder().id(dto.projectId()).build())
                .parentTask(dto.parentTaskId() != null ? Task.builder().id(dto.parentTaskId()).build() : null)
                .wbsNumber(dto.wbsNumber() != null ? dto.wbsNumber() : "1")
                .mode(dto.mode() != null ? dto.mode() : "TASK")
                .durationDays(dto.durationDays() != null ? dto.durationDays() : 1.0)
                .workHours(dto.workHours())
                .baselineDurationDays(dto.baselineDurationDays())
                .baselineStartDate(dto.baselineStartDate())
                .baselineEndDate(dto.baselineEndDate())
                .actualWorkHours(dto.actualWorkHours())
                .calendarName(dto.calendarName())
                .sortOrder(dto.sortOrder() != null ? dto.sortOrder() : 0)
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .status(dto.status())
                .progress(dto.progress() != null ? dto.progress() : 0)
                .active(forceActive ? true : dto.active())
                .build();
    }

    /** Valide la coherence des dates avant sauvegarde. */
    private void validateDates(TaskDTO dto) {
        if (dto.startDate() != null && dto.endDate() != null
                && dto.endDate().isBefore(dto.startDate())) {
            throw new InvalidOperationException("endDate cannot be before startDate");
        }
        if (dto.baselineStartDate() != null && dto.baselineEndDate() != null
                && dto.baselineEndDate().isBefore(dto.baselineStartDate())) {
            throw new InvalidOperationException("baselineEndDate cannot be before baselineStartDate");
        }
    }
}
