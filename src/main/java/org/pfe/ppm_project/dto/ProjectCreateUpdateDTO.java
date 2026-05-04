package org.pfe.ppm_project.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO pour la creation et la mise a jour d'un projet.
 * Contient les champs modifiables avec leurs contraintes de validation.
 * Les champs null lors d'une mise a jour conservent la valeur existante.
 */
public record ProjectCreateUpdateDTO(
        @Size(min = 2, max = 255, message = "Project name must be 2-255 characters")
        String name,                // Nom du projet (2-255 caracteres)
        @Size(max = 2000, message = "Description max 2000 characters")
        String description,         // Description (max 2000 caracteres)
        LocalDate startDate,        // Date de debut
        LocalDate endDate,          // Date de fin
        Boolean active,             // Statut actif/inactif
        Long projectManagerId,      // ID du chef de projet

        String portfolioName,       // Nom du portefeuille
        String programName,         // Nom du programme
        String subProgramName,      // Nom du sous-programme
        String objective,           // Objectif strategique
        String calendarName,        // Nom du calendrier
        LocalDate baselineStartDate, // Date de debut de reference
        LocalDate baselineEndDate,  // Date de fin de reference
        @Min(value = 0, message = "Progress must be 0-100") @Max(value = 100, message = "Progress must be 0-100")
        Integer progress,           // Pourcentage d'avancement (0-100)
        Long calendarId             // ID du calendrier de travail a associer
) {}
