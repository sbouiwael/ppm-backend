package org.pfe.ppm_project.services;

import org.pfe.ppm_project.entities.Task;

import java.util.List;
import java.util.Optional;

public interface ITaskService {

    Task createTask(Task task);

    List<Task> getTasksByProject(Long projectId);

    Optional<Task> getTaskById(Long id);

    Task updateTask(Long id, Task task);

    void deactivateTask(Long id);
}
