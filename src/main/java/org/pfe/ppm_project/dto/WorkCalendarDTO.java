package org.pfe.ppm_project.dto;

import java.util.List;

/**
 * DTO de lecture pour un calendrier de travail.
 * Inclut la configuration des jours ouvres et la liste des exceptions.
 */
public record WorkCalendarDTO(
        Long id,                                // Identifiant du calendrier
        String name,                            // Nom du calendrier
        String description,                     // Description
        Double workHoursPerDay,                 // Heures de travail par jour
        String workDays,                        // Jours ouvres (ex: "1,2,3,4,5" pour Lun-Ven)
        List<CalendarExceptionDTO> exceptions   // Liste des jours d'exception
) {}
