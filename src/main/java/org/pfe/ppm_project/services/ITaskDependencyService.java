package org.pfe.ppm_project.services;

import org.pfe.ppm_project.dto.TaskDependencyCreateRequest;
import org.pfe.ppm_project.dto.TaskDependencyDTO;

import java.util.List;

public interface ITaskDependencyService {

    TaskDependencyDTO createDependency(TaskDependencyCreateRequest req);

    List<TaskDependencyDTO> getPredecessorsOfTask(Long taskId);

    List<TaskDependencyDTO> getSuccessorsOfTask(Long taskId);

    void deleteDependency(Long id);
}
