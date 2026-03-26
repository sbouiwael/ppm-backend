package org.pfe.ppm_project.dto;

import java.time.LocalDate;

public record ProjectCreateUpdateDTO(
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        Boolean active,
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