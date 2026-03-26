package org.pfe.ppm_project.services;

import org.pfe.ppm_project.entities.TaskAssignment;

import java.util.List;

public interface ITaskAssignmentService {

    TaskAssignment assignUserToTask(TaskAssignment assignment);

    List<TaskAssignment> getAssignmentsByTask(Long taskId);

    List<TaskAssignment> getAssignmentsByUser(Long userId);

    TaskAssignment updateAssignedHours(Long assignmentId, Integer assignedHours);

    void deactivateAssignment(Long assignmentId);
}
