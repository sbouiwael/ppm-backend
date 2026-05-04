package org.pfe.ppm_project.services;

import org.pfe.ppm_project.dto.AuditLogDTO;
import org.pfe.ppm_project.enums.AuditAction;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

/**
 * Contrat du service de journal d'audit PPM.
 *
 * PRINCIPE : seules les mutations d'etat metier significatives sont loggees.
 * Les lectures (GET) ne generent pas d'entrees d'audit.
 */
public interface IAuditLogService {

    /**
     * Enregistre une action metier dans le journal d'audit.
     * Extrait automatiquement les informations de l'acteur depuis le SecurityContext.
     *
     * @param action      Nature de l'action (CREATE, UPDATE, DELETE, etc.)
     * @param entityType  Type de l'entite (PROJECT, TASK, PORTFOLIO, USER, ASSIGNMENT, DEPENDENCY)
     * @param entityId    Identifiant de l'entite concernee
     * @param entityName  Nom lisible de l'entite (denormalise pour l'historique)
     * @param details     Description lisible de l'action (ex: "Status: NOT_STARTED -> IN_PROGRESS")
     * @param projectId   ID du projet de contexte (null si non applicable)
     * @param projectName Nom du projet de contexte (null si non applicable)
     */
    void log(
            AuditAction action,
            String entityType,
            Long entityId,
            String entityName,
            String details,
            Long projectId,
            String projectName
    );

    /**
     * Recherche paginee avec filtres optionnels.
     * Tous les parametres sont optionnels (null = pas de filtre).
     */
    Page<AuditLogDTO> search(
            String entityType,
            Long actorId,
            AuditAction action,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    );

    /** Historique complet d'une entite (ex: toutes les modifs d'un projet). */
    java.util.List<AuditLogDTO> getByEntity(String entityType, Long entityId);

    /** Toutes les actions loggees dans le contexte d'un projet. */
    java.util.List<AuditLogDTO> getByProject(Long projectId);

    /** Toutes les actions effectuees par un acteur (ADMIN uniquement). */
    java.util.List<AuditLogDTO> getByActor(Long actorId);
}