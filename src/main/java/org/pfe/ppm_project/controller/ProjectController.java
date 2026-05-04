package org.pfe.ppm_project.controller;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.ProjectCreateUpdateDTO;
import org.pfe.ppm_project.dto.ProjectDTO;
import org.pfe.ppm_project.services.IProjectService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller CRUD des projets — gere la creation, lecture, modification et desactivation.
 * Les operations d'ecriture (POST, PUT, DELETE) sont reservees aux roles ADMIN, PMO et PM.
 * La lecture est ouverte a tous les utilisateurs authentifies.
 * La suppression est une desactivation logique (soft delete) — le projet reste en base.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    // Service metier des projets — contient la logique de creation, validation, etc.
    private final IProjectService projectService;

    // Creation d'un projet — seuls ADMIN, PMO et PM peuvent creer des projets
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')") // Controle d'acces base sur le role dans le token JWT
    public ResponseEntity<?> create(@Valid @RequestBody ProjectCreateUpdateDTO dto) {
        return projectService.create(dto)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body("Invalid project data or projectManagerId not found"));
    }

    // Liste de tous les projets — accessible a tous les utilisateurs authentifies
    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getAll() {
        return ResponseEntity.ok(projectService.getAll());
    }

    // Recuperation d'un projet par son ID
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getById(@PathVariable Long id) {
        return projectService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Liste des projets geres par un chef de projet specifique — utile pour le filtre par manager
    @GetMapping("/manager/{managerId}")
    public ResponseEntity<List<ProjectDTO>> getByManager(@PathVariable Long managerId) {
        return ResponseEntity.ok(projectService.getByManager(managerId));
    }

    // Mise a jour d'un projet existant — memes restrictions de role que la creation
    // PM : uniquement sur les projets dont il est le chef de projet designe
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ProjectCreateUpdateDTO dto) {
        enforceProjectOwnershipForPm(id);
        return projectService.update(id, dto)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body("Invalid update data or id not found"));
    }

    // Active ou desactive un projet sans modifier ses autres donnees
    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')")
    public ResponseEntity<Void> setActive(@PathVariable Long id, @RequestParam boolean active) {
        enforceProjectOwnershipForPm(id);
        projectService.setProjectActive(id, active);
        return ResponseEntity.noContent().build();
    }

    // Suppression physique d'un projet avec cascade (taches, dependances, affectations)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        enforceProjectOwnershipForPm(id);
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Verifie que si l'appelant est un PM (pas ADMIN ni PMO),
     * il est bien le chef de projet designe pour ce projet.
     * Lance AccessDeniedException sinon — Spring Security la mappe en 403.
     */
    private void enforceProjectOwnershipForPm(Long projectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isPmOnly = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PM"))
                && auth.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                             || a.getAuthority().equals("ROLE_PMO"));
        if (isPmOnly) {
            Long currentUserId = (Long) auth.getDetails();
            if (!projectService.isProjectManager(projectId, currentUserId)) {
                throw new AccessDeniedException("PM can only modify projects they manage");
            }
        }
    }
}
