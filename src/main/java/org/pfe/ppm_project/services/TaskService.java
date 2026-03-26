package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.repositories.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService implements ITaskService {

    private final TaskRepository taskRepository;

    @Override
    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    @Override
    public List<Task> getTasksByProject(Long projectId) {
        return taskRepository.findByProjectIdOrderBySortOrderAsc(projectId);
    }

    @Override
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    @Override
    public Task updateTask(Long id, Task updatedTask) {
        return taskRepository.findById(id)
                .map(task -> {
                    task.setName(updatedTask.getName());
                    task.setDescription(updatedTask.getDescription());
                    task.setProject(updatedTask.getProject());
                    task.setParentTask(updatedTask.getParentTask());
                    task.setDurationDays(updatedTask.getDurationDays());
                    task.setStartDate(updatedTask.getStartDate());
                    task.setEndDate(updatedTask.getEndDate());
                    task.setStatus(updatedTask.getStatus());
                    task.setProgress(updatedTask.getProgress());
                    task.setActive(updatedTask.isActive());
                    task.setWbsNumber(updatedTask.getWbsNumber());
                    task.setMode(updatedTask.getMode());
                    task.setWorkHours(updatedTask.getWorkHours());
                    task.setBaselineDurationDays(updatedTask.getBaselineDurationDays());
                    task.setBaselineStartDate(updatedTask.getBaselineStartDate());
                    task.setBaselineEndDate(updatedTask.getBaselineEndDate());
                    task.setActualWorkHours(updatedTask.getActualWorkHours());
                    task.setCalendarName(updatedTask.getCalendarName());
                    task.setSortOrder(updatedTask.getSortOrder());
                    return taskRepository.save(task);
                })
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }

    @Override
    public void deactivateTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        task.setActive(false);
        taskRepository.save(task);
    }
}
