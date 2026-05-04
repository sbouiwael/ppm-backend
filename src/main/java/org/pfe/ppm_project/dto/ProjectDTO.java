package org.pfe.ppm_project.dto;

import java.time.LocalDate;

/**
 * DTO de lecture pour un projet.
 * Transfere toutes les informations d'un projet vers le client
 * (identifiant, dates, avancement, references portefeuille et calendrier).
 */
public record ProjectDTO(
        Long id,                    // Identifiant du projet
        String name,                // Nom du projet
        String description,         // Description du projet
        LocalDate startDate,        // Date de debut
        LocalDate endDate,          // Date de fin prevue
        boolean active,             // Projet actif ou desactive
        Long projectManagerId,      // ID du chef de projet
        String projectManagerName,  // Nom complet du chef de projet (pour affichage)

        // Champs portefeuille/programme
        String portfolioName,       // Nom du portefeuille
        String programName,         // Nom du programme
        String subProgramName,      // Nom du sous-programme
        String objective,           // Objectif strategique
        String calendarName,        // Nom du calendrier
        LocalDate baselineStartDate, // Date de debut de reference
        LocalDate baselineEndDate,  // Date de fin de reference
        Integer progress,           // Pourcentage d'avancement (0-100)
        Long portefeuilleId,        // ID du portefeuille rattache
        Long calendarId             // ID du calendrier de travail
) {}
