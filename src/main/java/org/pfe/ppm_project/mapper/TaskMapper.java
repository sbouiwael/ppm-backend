package org.pfe.ppm_project.mapper;

import org.pfe.ppm_project.dto.TaskDTO;
import org.pfe.ppm_project.entities.Task;

import java.util.List;

/**
 * Mapper utilitaire pour convertir l'entite Task en TaskDTO.
 * Extrait les identifiants des relations (projet, tache parente) au lieu des objets complets.
 *
 * Trois variantes :
 *   toDTO(Task)                                — usage simple, assignedUserIds=null, hasChildren=false
 *   toDTO(Task, List<Long>)                    — usage detail sans hasChildren
 *   toDTO(Task, List<Long>, boolean)           — usage complet avec hasChildren
 */
public final class TaskMapper {
    private TaskMapper() {}

    /** Conversion minimale (tests, creation). */
    public static TaskDTO toDTO(Task t) {
        return toDTO(t, null, false);
    }

    /** Conversion avec IDs ressources (GET /api/tasks/{id}). hasChildren doit etre calcule par l'appelant. */
    public static TaskDTO toDTO(Task t, List<Long> assignedUserIds) {
        return toDTO(t, assignedUserIds, false);
    }

    /**
     * Conversion complete avec IDs ressources et indicateur de sous-taches.
     *
     * @param assignedUserIds liste des userId actifs (peut etre null)
     * @param hasChildren     true si cette tache a au moins un enfant actif
     */
    public static TaskDTO toDTO(Task t, List<Long> assignedUserIds, boolean hasChildren) {
        String projectName    = t.getProject()    != null ? t.getProject().getName()    : null;
        String parentTaskName = t.getParentTask() != null ? t.getParentTask().getName() : null;

        return new TaskDTO(
                t.getId(),
                t.getName(),
                t.getDescription(),
                t.getProject() != null ? t.getProject().getId() : null,
                t.getParentTask() != null ? t.getParentTask().getId() : null,

                t.getWbsNumber(),
                t.getMode(),

                t.getDurationDays(),
                t.getWorkHours(),

                t.getBaselineDurationDays(),
                t.getBaselineStartDate(),
                t.getBaselineEndDate(),

                t.getActualWorkHours(),
                t.getCalendarName(),

                t.getSortOrder(),

                t.getStartDate(),
                t.getEndDate(),

                t.getStatus(),
                t.getProgress(),
                t.isActive(),
                t.getCreatedAt(),

                projectName,
                parentTaskName,
                assignedUserIds,
                hasChildren
        );
    }
}
