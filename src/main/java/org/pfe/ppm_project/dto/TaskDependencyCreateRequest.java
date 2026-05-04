package org.pfe.ppm_project.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.pfe.ppm_project.enums.DependencyType;

/**
 * DTO pour la creation d'une dependance entre deux taches.
 * Le type est optionnel (par defaut FS = Fin-Debut).
 */
public record TaskDependencyCreateRequest(
        @NotNull(message = "Predecessor task ID is required")
        @Positive(message = "Predecessor task ID must be positive")
        Long predecessorTaskId,

        @NotNull(message = "Successor task ID is required")
        @Positive(message = "Successor task ID must be positive")
        Long successorTaskId,

        DependencyType type         // Type de dependance (optionnel, defaut = FS)
) {}
