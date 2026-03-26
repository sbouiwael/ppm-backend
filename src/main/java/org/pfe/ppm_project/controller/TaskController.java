package org.pfe.ppm_project.controller;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.TaskDTO;
import org.pfe.ppm_project.entities.Project;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.mapper.TaskMapper;
import org.pfe.ppm_project.services.ITaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final ITaskService taskService;

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(@RequestBody TaskDTO dto) {
        Task task = Task.builder()
                .name(dto.name())
                .description(dto.description())
                .project(Project.builder().id(dto.projectId()).build())
                .parentTask(dto.parentTaskId() != null ? Task.builder().id(dto.parentTaskId()).build() : null)

                .wbsNumber(dto.wbsNumber() != null ? dto.wbsNumber() : "1")
                .mode(dto.mode() != null ? dto.mode() : "TASK")

                .durationDays(dto.durationDays() != null ? dto.durationDays() : 1.0)
                .workHours(dto.workHours()) // may be null -> normalized in entity

                .baselineDurationDays(dto.baselineDurationDays())
                .baselineStartDate(dto.baselineStartDate())
                .baselineEndDate(dto.baselineEndDate())

                .actualWorkHours(dto.actualWorkHours())
                .calendarName(dto.calendarName())

                .sortOrder(dto.sortOrder() != null ? dto.sortOrder() : 0)

                .startDate(dto.startDate())
                .endDate(dto.endDate())

                .status(dto.status())
                .progress(dto.progress() != null ? dto.progress() : 0)
                .active(true)
                .build();

        Task created = taskService.createTask(task);
        return new ResponseEntity<>(TaskMapper.toDTO(created), HttpStatus.CREATED);
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskDTO>> getByProject(@PathVariable Long projectId) {
        List<TaskDTO> list = taskService.getTasksByProject(projectId)
                .stream().map(TaskMapper::toDTO).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getById(@PathVariable Long id) {
        return taskService.getTaskById(id)
                .map(TaskMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> update(@PathVariable Long id, @RequestBody TaskDTO dto) {
        Task updated = Task.builder()
                .name(dto.name())
                .description(dto.description())
                .project(Project.builder().id(dto.projectId()).build())
                .parentTask(dto.parentTaskId() != null ? Task.builder().id(dto.parentTaskId()).build() : null)

                .wbsNumber(dto.wbsNumber())
                .mode(dto.mode())

                .durationDays(dto.durationDays())
                .workHours(dto.workHours())

                .baselineDurationDays(dto.baselineDurationDays())
                .baselineStartDate(dto.baselineStartDate())
                .baselineEndDate(dto.baselineEndDate())

                .actualWorkHours(dto.actualWorkHours())
                .calendarName(dto.calendarName())

                .sortOrder(dto.sortOrder())

                .startDate(dto.startDate())
                .endDate(dto.endDate())

                .status(dto.status())
                .progress(dto.progress())
                .active(dto.active())
                .build();

        return ResponseEntity.ok(TaskMapper.toDTO(taskService.updateTask(id, updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        taskService.deactivateTask(id);
        return ResponseEntity.noContent().build();
    }
}
