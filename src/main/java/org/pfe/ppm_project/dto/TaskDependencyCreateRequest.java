package org.pfe.ppm_project.dto;

import org.pfe.ppm_project.enums.DependencyType;

public record TaskDependencyCreateRequest(
        Long predecessorTaskId,
        Long successorTaskId,
        DependencyType type
) {}
