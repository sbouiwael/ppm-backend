package org.pfe.ppm_project.services;

import org.pfe.ppm_project.dto.MyTaskDTO;
import org.pfe.ppm_project.entities.TaskAssignment;

import java.util.List;

public interface ITaskAssignmentService {

    TaskAssignment assignUserToTask(TaskAssignment assignment);

    List<TaskAssignment> getAssignmentsByTask(Long taskId);

    List<TaskAssignment> getAssignmentsByUser(Long userId);

    TaskAssignment updateAssignedHours(Long assignmentId, Integer assignedHours);

    void deactivateAssignment(Long assignmentId);

    /**
     * Retourne la liste des taches affectees a l'utilisateur identifie par userId.
     * Seules les affectations actives sur des taches actives sont incluses.
     * Logique Microsoft PPM : "My Tasks" — vue personnelle du contributeur.
     */
    List<MyTaskDTO> getMyTasks(Long userId);
}
