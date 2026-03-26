package org.pfe.ppm_project.dto;

import java.time.LocalDate;

public record ProjectDTO(
        Long id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        Long projectManagerId,

        // NEW
        String portfolioName,
        String programName,
        String subProgramName,
        String objective,
        String calendarName,
        LocalDate baselineStartDate,
        LocalDate baselineEndDate,
        Integer progress
) {}