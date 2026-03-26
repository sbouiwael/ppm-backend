package org.pfe.ppm_project.mapper;

import org.pfe.ppm_project.dto.TaskDTO;
import org.pfe.ppm_project.entities.Task;

public final class TaskMapper {
    private TaskMapper() {}

    public static TaskDTO toDTO(Task t) {
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
                t.getCreatedAt()
        );
    }
}