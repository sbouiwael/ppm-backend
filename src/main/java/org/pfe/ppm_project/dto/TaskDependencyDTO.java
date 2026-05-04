package org.pfe.ppm_project.dto;

import org.pfe.ppm_project.enums.DependencyType;

import java.time.LocalDateTime;

/**
 * DTO de lecture pour une dependance entre taches.
 * Retourne les identifiants et noms des taches predecesseur et successeur
 * ainsi que le type de lien (FS, SS, FF, SF).
 */
public record TaskDependencyDTO(
        Long id,                        // Identifiant de la dependance
        Long predecessorTaskId,         // ID de la tache predecesseur
        String predecessorTaskName,     // Nom de la tache predecesseur (pour affichage)
        Long successorTaskId,           // ID de la tache successeur
        String successorTaskName,       // Nom de la tache successeur (pour affichage)
        DependencyType type,            // Type de dependance (FS, SS, FF, SF)
        LocalDateTime createdAt         // Date de creation
) {}
