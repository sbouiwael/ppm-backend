package org.pfe.ppm_project.controller;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.AuditLogDTO;
import org.pfe.ppm_project.enums.AuditAction;
import org.pfe.ppm_project.services.IAuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller REST pour le journal d'audit PPM.
 *
 * RBAC :
 *   ADMIN / PMO — acces global : search, entity history, project audit, actor history
 *   PM          — acces scoped : uniquement l'historique de ses propres projets
 *                (le filtrage fin par ownership est gere cote frontend via projectId)
 *
 * Toutes les operations sont en lecture seule — les entrees d'audit ne peuvent
 * pas etre modifiees ou supprimees via l'API (immuabilite pour la gouvernance).
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final IAuditLogService auditLogService;

    /**
     * Recherche paginee avec filtres optionnels.
     * Accessible a ADMIN et PMO uniquement.
     *
     * Query params (tous optionnels) :
     *   entityType — ex: PROJECT, TASK, PORTFOLIO, USER, ASSIGNMENT, DEPENDENCY
     *   actorId    — ID de l'utilisateur ayant effectue l'action
     *   action     — CREATE, UPDATE, DELETE, STATUS_CHANGE, ASSIGN, UNASSIGN
     *   from       — ISO datetime debut de plage (ex: 2026-01-01T00:00:00)
     *   to         — ISO datetime fin de plage
     *   page       — numero de page (0-based, default 0)
     *   size       — taille de page (default 20, max 100)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PMO')")
    public ResponseEntity<Page<AuditLogDTO>> search(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                auditLogService.search(entityType, actorId, action, from, to, page, size));
    }

    /**
     * Historique complet d'une entite specifique.
     * Ex: GET /api/audit/entity/PROJECT/42 → toutes les actions sur le projet 42.
     * Accessible a ADMIN, PMO et PM.
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')")
    public ResponseEntity<List<AuditLogDTO>> getByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId
    ) {
        return ResponseEntity.ok(auditLogService.getByEntity(entityType, entityId));
    }

    /**
     * Toutes les actions loggees dans le contexte d'un projet.
     * Ex: GET /api/audit/project/42 → audit trail complet du projet 42.
     * Accessible a ADMIN, PMO et PM.
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')")
    public ResponseEntity<List<AuditLogDTO>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(auditLogService.getByProject(projectId));
    }

    /**
     * Toutes les actions effectuees par un acteur donne.
     * Ex: GET /api/audit/actor/5 → toutes les actions de l'utilisateur 5.
     * Accessible a ADMIN uniquement (vue gouvernance globale).
     */
    @GetMapping("/actor/{actorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogDTO>> getByActor(@PathVariable Long actorId) {
        return ResponseEntity.ok(auditLogService.getByActor(actorId));
    }
}