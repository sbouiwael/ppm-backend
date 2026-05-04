package org.pfe.ppm_project.dto;

import org.pfe.ppm_project.enums.AuditAction;

import java.time.LocalDateTime;

/**
 * DTO de lecture pour une entree du journal d'audit.
 * Toutes les donnees sont denormalisees pour un affichage direct sans N+1.
 */
public record AuditLogDTO(
        Long id,
        Long actorId,
        String actorEmail,
        String actorRole,
        AuditAction action,
        String entityType,
        Long entityId,
        String entityName,
        String details,
        Long projectId,
        String projectName,
        LocalDateTime timestamp
) {}