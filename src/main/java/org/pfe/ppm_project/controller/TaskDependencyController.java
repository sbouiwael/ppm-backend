package org.pfe.ppm_project.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.TaskDependencyCreateRequest;
import org.pfe.ppm_project.dto.TaskDependencyDTO;
import org.pfe.ppm_project.services.ITaskDependencyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller des dependances entre taches — gere les liens predecesseur/successeur.
 * Les dependances sont essentielles pour le diagramme de Gantt et la planification.
 * Exemple : la tache B ne peut commencer qu'apres la fin de la tache A (Finish-to-Start).
 * Seuls ADMIN, PMO et PM peuvent creer ou supprimer des dependances.
 */
@RestController
@RequestMapping("/api/dependencies")
@RequiredArgsConstructor
public class TaskDependencyController {

    private final ITaskDependencyService dependencyService;

    // Creation d'une dependance entre deux taches (predecesseur -> successeur)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')")
    public ResponseEntity<TaskDependencyDTO> create(@Valid @RequestBody TaskDependencyCreateRequest req) {
        TaskDependencyDTO created = dependencyService.createDependency(req);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // Liste des predecesseurs d'une tache — quelles taches doivent finir avant celle-ci
    // RH exclu : la gestion des dependances n'est pas dans son perimetre
    @GetMapping("/predecessors/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM','DEV','QA','DEVOPS')")
    public ResponseEntity<List<TaskDependencyDTO>> predecessors(@PathVariable Long taskId) {
        return ResponseEntity.ok(dependencyService.getPredecessorsOfTask(taskId));
    }

    // Liste des successeurs d'une tache — quelles taches dependent de celle-ci
    @GetMapping("/successors/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM','DEV','QA','DEVOPS')")
    public ResponseEntity<List<TaskDependencyDTO>> successors(@PathVariable Long taskId) {
        return ResponseEntity.ok(dependencyService.getSuccessorsOfTask(taskId));
    }

    // Toutes les dependances d'un projet — necessaire pour dessiner les liens dans le Gantt
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM','DEV','QA','DEVOPS')")
    public ResponseEntity<List<TaskDependencyDTO>> byProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(dependencyService.getByProject(projectId));
    }

    // Suppression d'une dependance — suppression reelle (pas de soft delete)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dependencyService.deleteDependency(id);
        return ResponseEntity.noContent().build();
    }
}
