package org.pfe.ppm_project.dto;

import org.pfe.ppm_project.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskDTO(
        Long id,
        String name,
        String description,
        Long projectId,
        Long parentTaskId,

        // NEW aligned fields
        String wbsNumber,
        String mode,

        Double durationDays,
        Double workHours,

        Double baselineDurationDays,
        LocalDate baselineStartDate,
        LocalDate baselineEndDate,

        Double actualWorkHours,
        String calendarName,

        Integer sortOrder,

        LocalDate startDate,
        LocalDate endDate,

        TaskStatus status,
        Integer progress,
        boolean active,
        LocalDateTime createdAt
) {}