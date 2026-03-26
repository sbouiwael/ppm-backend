package org.pfe.ppm_project.dto;

import java.time.LocalDateTime;

public record TaskAssignmentDTO(
        Long id,
        Long taskId,
        Long userId,
        Integer assignedHours,
        boolean active,
        LocalDateTime createdAt
) {}
