package org.pfe.ppm_project.controller;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.TaskAssignmentDTO;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.entities.TaskAssignment;
import org.pfe.ppm_project.entities.User;
import org.pfe.ppm_project.mapper.TaskAssignmentMapper;
import org.pfe.ppm_project.services.ITaskAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class TaskAssignmentController {

    private final ITaskAssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<TaskAssignmentDTO> assign(@RequestBody TaskAssignmentDTO dto) {
        TaskAssignment entity = TaskAssignment.builder()
                .task(Task.builder().id(dto.taskId()).build())
                .user(User.builder().id(dto.userId()).build())
                .assignedHours(dto.assignedHours())
                .build();

        TaskAssignment created = assignmentService.assignUserToTask(entity);
        return new ResponseEntity<>(TaskAssignmentMapper.toDTO(created), HttpStatus.CREATED);
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TaskAssignmentDTO>> byTask(@PathVariable Long taskId) {
        List<TaskAssignmentDTO> list = assignmentService.getAssignmentsByTask(taskId)
                .stream().map(TaskAssignmentMapper::toDTO).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TaskAssignmentDTO>> byUser(@PathVariable Long userId) {
        List<TaskAssignmentDTO> list = assignmentService.getAssignmentsByUser(userId)
                .stream().map(TaskAssignmentMapper::toDTO).toList();
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{assignmentId}/hours/{hours}")
    public ResponseEntity<TaskAssignmentDTO> updateHours(
            @PathVariable Long assignmentId,
            @PathVariable Integer hours) {

        TaskAssignment updated = assignmentService.updateAssignedHours(assignmentId, hours);
        return ResponseEntity.ok(TaskAssignmentMapper.toDTO(updated));
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> deactivate(@PathVariable Long assignmentId) {
        assignmentService.deactivateAssignment(assignmentId);
        return ResponseEntity.noContent().build();
    }
}
