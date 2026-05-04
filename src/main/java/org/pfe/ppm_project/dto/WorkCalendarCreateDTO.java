package org.pfe.ppm_project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * DTO pour la creation et la mise a jour d'un calendrier de travail.
 * Le nom est obligatoire. Les autres champs utilisent des valeurs par defaut si non fournis.
 */
public record WorkCalendarCreateDTO(
        @NotBlank(message = "Calendar name is required")
        String name,            // Nom du calendrier (obligatoire)
        String description,     // Description (optionnelle)
        @Positive(message = "Work hours per day must be positive")
        Double workHoursPerDay, // Heures de travail par jour (defaut: 8.0)
        String workDays         // Jours ouvres, ex: "1,2,3,4,5" (defaut: Lun-Ven)
) {}
