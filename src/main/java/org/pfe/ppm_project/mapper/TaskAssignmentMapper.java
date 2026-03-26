package org.pfe.ppm_project.mapper;

import org.pfe.ppm_project.dto.TaskAssignmentDTO;
import org.pfe.ppm_project.entities.TaskAssignment;

public final class TaskAssignmentMapper {
    private TaskAssignmentMapper() {}

    public static TaskAssignmentDTO toDTO(TaskAssignment ta) {
        return new TaskAssignmentDTO(
                ta.getId(),
                ta.getTask() != null ? ta.getTask().getId() : null,
                ta.getUser() != null ? ta.getUser().getId() : null,
                ta.getAssignedHours(),
                ta.isActive(),
                ta.getCreatedAt()
        );
    }
}
