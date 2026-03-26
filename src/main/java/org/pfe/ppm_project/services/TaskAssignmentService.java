package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.entities.TaskAssignment;
import org.pfe.ppm_project.entities.User;
import org.pfe.ppm_project.repositories.TaskAssignmentRepository;
import org.pfe.ppm_project.repositories.TaskRepository;
import org.pfe.ppm_project.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskAssignmentService implements ITaskAssignmentService {

    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Override
    public TaskAssignment assignUserToTask(TaskAssignment assignment) {
        if (assignment == null) throw new RuntimeException("assignment is required");
        if (assignment.getTask() == null || assignment.getTask().getId() == null)
            throw new RuntimeException("taskId is required");
        if (assignment.getUser() == null || assignment.getUser().getId() == null)
            throw new RuntimeException("userId is required");
        if (assignment.getAssignedHours() == null || assignment.getAssignedHours() < 0)
            throw new RuntimeException("assignedHours must be >= 0");

        Long taskId = assignment.getTask().getId();
        Long userId = assignment.getUser().getId();

        if (taskAssignmentRepository.existsByTaskIdAndUserId(taskId, userId)) {
            throw new RuntimeException("This user is already assigned to this task");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Double plannedWork = task.getWorkHours();
        if (plannedWork == null) throw new RuntimeException("Task workHours is null");

// assignedHours is Integer, so compare with a rounded/converted ceiling
        int workloadLimit = (int) Math.ceil(plannedWork);

        int alreadyAssigned = taskAssignmentRepository.sumActiveAssignedHoursByTask(taskId);
        if (alreadyAssigned + assignment.getAssignedHours() > workloadLimit) {
            throw new RuntimeException("Total assigned hours exceed task planned work (" + workloadLimit + "h)");
        }

        assignment.setTask(task);
        assignment.setUser(user);
        assignment.setActive(true);

        return taskAssignmentRepository.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAssignment> getAssignmentsByTask(Long taskId) {
        return taskAssignmentRepository.findByTaskIdWithRefs(taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAssignment> getAssignmentsByUser(Long userId) {
        return taskAssignmentRepository.findByUserIdWithRefs(userId);
    }

    @Override
    public TaskAssignment updateAssignedHours(Long assignmentId, Integer assignedHours) {
        if (assignedHours == null || assignedHours < 0)
            throw new RuntimeException("assignedHours must be >= 0");

        TaskAssignment existing = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));

        if (!existing.isActive()) {
            throw new RuntimeException("Cannot update hours of an inactive assignment");
        }

        Long taskId = existing.getTask().getId();
        Double plannedWork = existing.getTask().getWorkHours();
        if (plannedWork == null) throw new RuntimeException("Task workHours is null");

        int workloadLimit = (int) Math.ceil(plannedWork);

        int othersAssigned = taskAssignmentRepository.sumActiveAssignedHoursByTaskExcluding(taskId, assignmentId);
        if (othersAssigned + assignedHours > workloadLimit) {
            throw new RuntimeException("Total assigned hours exceed task planned work (" + workloadLimit + "h)");
        }

        existing.setAssignedHours(assignedHours);
        return taskAssignmentRepository.save(existing);
    }

    @Override
    public void deactivateAssignment(Long assignmentId) {
        TaskAssignment existing = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));

        existing.setActive(false);
        taskAssignmentRepository.save(existing);
    }
}
