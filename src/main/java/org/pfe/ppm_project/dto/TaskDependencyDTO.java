package org.pfe.ppm_project.dto;

import org.pfe.ppm_project.enums.DependencyType;

import java.time.LocalDateTime;

public record TaskDependencyDTO(
        Long id,
        Long predecessorTaskId,
        Long successorTaskId,
        DependencyType type,
        LocalDateTime createdAt
) {}
