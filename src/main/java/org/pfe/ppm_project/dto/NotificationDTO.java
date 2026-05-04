package org.pfe.ppm_project.dto;

import org.pfe.ppm_project.enums.NotificationType;

import java.time.LocalDateTime;

/**
 * DTO de lecture pour une notification utilisateur.
 * Toutes les informations necessaires a l'affichage sont incluses.
 */
public record NotificationDTO(
        Long id,
        Long recipientId,
        NotificationType type,
        String title,
        String message,
        boolean read,
        String entityType,
        Long entityId,
        LocalDateTime createdAt
) {}