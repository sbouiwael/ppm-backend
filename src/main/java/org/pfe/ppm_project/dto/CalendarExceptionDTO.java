package org.pfe.ppm_project.dto;

import java.time.LocalDate;

/**
 * DTO pour une exception de calendrier (jour ferie ou jour ouvre exceptionnel).
 * Utilise en lecture et en creation d'exception sur un calendrier de travail.
 */
public record CalendarExceptionDTO(
        Long id,                // Identifiant de l'exception
        Long calendarId,        // ID du calendrier parent
        LocalDate date,         // Date de l'exception
        String name,            // Nom/libelle (ex: "Fete nationale")
        boolean working,        // true = jour travaille exceptionnellement, false = jour ferie
        Double workHours        // Heures de travail pour ce jour (0 si non travaille)
) {}
