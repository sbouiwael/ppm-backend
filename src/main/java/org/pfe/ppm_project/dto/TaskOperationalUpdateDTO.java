package org.pfe.ppm_project.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.pfe.ppm_project.enums.TaskStatus;

/**
 * DTO restreint pour la mise a jour operationnelle d'une tache
 * par les membres de l'equipe (DEV, QA, DEVOPS).
 *
 * Seuls les champs liés au suivi d'avancement sont modifiables :
 *   - status  : nouveau statut de la tache
 *   - progress : pourcentage d'avancement (0-100)
 *   - actualWorkHours : heures reellement travaillees
 *
 * L'utilisation de ce DTO garantit que les membres techniques
 * ne peuvent jamais modifier les champs de planification
 * (nom, dates, duree, parent, WBS, etc.) via ce canal.
 */
public record TaskOperationalUpdateDTO(

        TaskStatus status,

        @Min(value = 0, message = "Progress must be between 0 and 100")
        @Max(value = 100, message = "Progress must be between 0 and 100")
        Integer progress,

        @Min(value = 0, message = "Actual work hours cannot be negative")
        Double actualWorkHours
) {}
