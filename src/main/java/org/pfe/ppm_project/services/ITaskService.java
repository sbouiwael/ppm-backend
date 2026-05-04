package org.pfe.ppm_project.services;

import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.enums.TaskStatus;

import java.util.List;
import java.util.Optional;

public interface ITaskService {

    Task createTask(Task task);

    List<Task> getTasksByProject(Long projectId);

    Optional<Task> getTaskById(Long id);

    /** Mise a jour complete (ADMIN / PMO / PM uniquement). */
    Task updateTask(Long id, Task task);

    /**
     * Mise a jour rapide du statut uniquement — Quick Actions My Tasks (DEV/QA/DEVOPS).
     * Propage le statut et le progres WBS vers les taches parentes.
     */
    Task updateTaskStatus(Long id, TaskStatus status);

    /**
     * Mise a jour operationnelle restreinte — endpoint dedie DEV/QA/DEVOPS.
     * Seuls status, progress et actualWorkHours peuvent etre modifies.
     * Propage progress, status et work-hours vers les taches parentes.
     */
    Task updateTaskOperational(Long id, TaskStatus status, Integer progress, Double actualWorkHours);

    void deactivateTask(Long id);

    void setTaskActive(Long id, boolean active);

    void deleteTask(Long id);
}
