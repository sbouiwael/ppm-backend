package org.pfe.ppm_project.controller;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.TaskDependencyCreateRequest;
import org.pfe.ppm_project.dto.TaskDependencyDTO;
import org.pfe.ppm_project.services.ITaskDependencyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dependencies")
@RequiredArgsConstructor
public class TaskDependencyController {

    private final ITaskDependencyService dependencyService;

    @PostMapping
    public ResponseEntity<TaskDependencyDTO> create(@RequestBody TaskDependencyCreateRequest req) {
        TaskDependencyDTO created = dependencyService.createDependency(req);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/predecessors/{taskId}")
    public ResponseEntity<List<TaskDependencyDTO>> predecessors(@PathVariable Long taskId) {
        return ResponseEntity.ok(dependencyService.getPredecessorsOfTask(taskId));
    }

    @GetMapping("/successors/{taskId}")
    public ResponseEntity<List<TaskDependencyDTO>> successors(@PathVariable Long taskId) {
        return ResponseEntity.ok(dependencyService.getSuccessorsOfTask(taskId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dependencyService.deleteDependency(id);
        return ResponseEntity.noContent().build();
    }
}
