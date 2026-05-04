package org.pfe.ppm_project.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.MyTaskDTO;
import org.pfe.ppm_project.dto.TaskAssignmentDTO;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.entities.TaskAssignment;
import org.pfe.ppm_project.entities.User;
import org.pfe.ppm_project.mapper.TaskAssignmentMapper;
import org.pfe.ppm_project.services.ITaskAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller d'affectation des ressources aux taches.
 * Gere le lien entre les utilisateurs et les taches avec un nombre d'heures assignees.
 * Permet de suivre la charge de travail de chaque membre de l'equipe.
 * Seuls ADMIN, PMO et PM peuvent creer, modifier ou desactiver des affectations.
 */
@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class TaskAssignmentController {

    private final ITaskAssignmentService assignmentService;

    /**
     * Retourne les taches affectees a l'utilisateur actuellement authentifie.
     * L'userId est extrait du SecurityContext (injecte par JwtAuthenticationFilter via setDetails).
     * Accessible a tous les utilisateurs authentifies — chacun voit uniquement ses propres taches.
     *
     * Logique Microsoft PPM : "My Tasks" — espace de travail personnel du contributeur.
     * Les taches retournees sont enrichies avec le nom du projet pour permettre
     * la navigation directe vers le projet parent.
     */
    @GetMapping("/me")
    public ResponseEntity<List<MyTaskDTO>> myTasks() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // L'userId est stocke dans les details du token par JwtAuthenticationFilter (authToken.setDetails(userId))
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(assignmentService.getMyTasks(userId));
    }

    // Affectation d'un utilisateur a une tache avec un nombre d'heures
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')") // Seuls les gestionnaires peuvent affecter des ressources
    public ResponseEntity<TaskAssignmentDTO> assign(@Valid @RequestBody TaskAssignmentDTO dto) {
        // Construction de l'entite a partir du DTO — utilise des references par ID (pas de chargement complet)
        TaskAssignment entity = TaskAssignment.builder()
                .task(Task.builder().id(dto.taskId()).build())
                .user(User.builder().id(dto.userId()).build())
                .assignedHours(dto.assignedHours())
                .build();

        TaskAssignment created = assignmentService.assignUserToTask(entity);
        return new ResponseEntity<>(TaskAssignmentMapper.toDTO(created), HttpStatus.CREATED);
    }

    // Liste des affectations pour une tache donnee — pour voir qui travaille sur quoi
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TaskAssignmentDTO>> byTask(@PathVariable Long taskId) {
        List<TaskAssignmentDTO> list = assignmentService.getAssignmentsByTask(taskId)
                .stream().map(TaskAssignmentMapper::toDTO).toList();
        return ResponseEntity.ok(list);
    }

    // Liste des affectations pour un utilisateur — pour voir la charge de travail d'une personne
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TaskAssignmentDTO>> byUser(@PathVariable Long userId) {
        List<TaskAssignmentDTO> list = assignmentService.getAssignmentsByUser(userId)
                .stream().map(TaskAssignmentMapper::toDTO).toList();
        return ResponseEntity.ok(list);
    }

    // Modification du nombre d'heures assignees — permet d'ajuster la charge sans recreer l'affectation
    @PutMapping("/{assignmentId}/hours/{hours}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')")
    public ResponseEntity<TaskAssignmentDTO> updateHours(
            @PathVariable Long assignmentId,
            @PathVariable Integer hours) {

        TaskAssignment updated = assignmentService.updateAssignedHours(assignmentId, hours);
        return ResponseEntity.ok(TaskAssignmentMapper.toDTO(updated));
    }

    // Desactivation logique d'une affectation (soft delete) — conserve l'historique
    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')")
    public ResponseEntity<Void> deactivate(@PathVariable Long assignmentId) {
        assignmentService.deactivateAssignment(assignmentId);
        return ResponseEntity.noContent().build();
    }
}
