package org.pfe.ppm_project.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * DTO de lecture/ecriture pour une affectation de ressource a une tache.
 * Les contraintes de validation (@NotNull, @Min) sont utilisees lors de la creation (POST).
 * Les champs id, active, createdAt sont ignores en ecriture (geres par le service).
 */
public record TaskAssignmentDTO(
        Long id,                    // Identifiant de l'affectation (null en creation)

        @NotNull(message = "taskId is required")
        @Min(value = 1, message = "taskId must be positive")
        Long taskId,                // ID de la tache concernee

        @NotNull(message = "userId is required")
        @Min(value = 1, message = "userId must be positive")
        Long userId,                // ID de l'utilisateur affecte

        String userName,            // Nom complet de l'utilisateur (lecture uniquement)

        @NotNull(message = "assignedHours is required")
        @Min(value = 0, message = "assignedHours cannot be negative")
        Integer assignedHours,      // Nombre d'heures assignees

        boolean active,             // Affectation active ou desactivee
        LocalDateTime createdAt     // Date de creation de l'affectation
) {}
