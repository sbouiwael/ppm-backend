package org.pfe.ppm_project.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.PortefeuilleCreateUpdateDTO;
import org.pfe.ppm_project.dto.PortefeuilleDTO;
import org.pfe.ppm_project.dto.ProjectDTO;
import org.pfe.ppm_project.services.IPortefeuilleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller CRUD des portefeuilles de projets — regroupe plusieurs projets sous un meme portefeuille.
 * Un portefeuille permet au PMO de gerer un ensemble de projets lies (meme departement, meme programme, etc.).
 * La lecture est ouverte aux ADMIN, PMO, PM et RH.
 * L'ecriture et la gestion des projets dans un portefeuille sont reservees aux ADMIN et PMO.
 */
@RestController
@RequestMapping("/api/portefeuilles")
@RequiredArgsConstructor
public class PortefeuilleController {

    private final IPortefeuilleService portefeuilleService;

    // Liste de tous les portefeuilles — accessible en lecture aux roles de gestion
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM','RH')")
    public ResponseEntity<List<PortefeuilleDTO>> getAll() {
        return ResponseEntity.ok(portefeuilleService.getAll());
    }

    // Recuperation d'un portefeuille par son ID avec ses projets associes
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM','RH')")
    public ResponseEntity<PortefeuilleDTO> getById(@PathVariable Long id) {
        return portefeuilleService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Liste des projets non assignes a un portefeuille — utile pour l'interface d'ajout de projets
    @GetMapping("/unassigned-projects")
    @PreAuthorize("hasAnyRole('ADMIN','PMO')")
    public ResponseEntity<List<ProjectDTO>> getUnassignedProjects() {
        return ResponseEntity.ok(portefeuilleService.getUnassignedProjects());
    }

    // Creation d'un portefeuille — reserve aux ADMIN et PMO
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PMO')")
    public ResponseEntity<?> create(@Valid @RequestBody PortefeuilleCreateUpdateDTO dto) {
        return portefeuilleService.create(dto)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body("Invalid portfolio data"));
    }

    // Mise a jour d'un portefeuille existant
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO')")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody PortefeuilleCreateUpdateDTO dto) {
        return portefeuilleService.update(id, dto)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body("Invalid update data or id not found"));
    }

    // Suppression d'un portefeuille (suppression reelle, pas de soft delete ici)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean ok = portefeuilleService.delete(id);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // Ajout d'un projet existant dans un portefeuille — associe un projet a ce portefeuille
    @PostMapping("/{id}/projects/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO')")
    public ResponseEntity<?> addProject(@PathVariable Long id, @PathVariable Long projectId) {
        return portefeuilleService.addProject(id, projectId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body("Cannot assign project."));
    }

    // Retrait d'un projet d'un portefeuille — dissocie le projet sans le supprimer
    @DeleteMapping("/{id}/projects/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO')")
    public ResponseEntity<?> removeProject(@PathVariable Long id, @PathVariable Long projectId) {
        return portefeuilleService.removeProject(id, projectId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body("Cannot remove project."));
    }
}
