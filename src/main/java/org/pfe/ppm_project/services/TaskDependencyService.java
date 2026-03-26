package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.TaskDependencyCreateRequest;
import org.pfe.ppm_project.dto.TaskDependencyDTO;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.entities.TaskDependency;
import org.pfe.ppm_project.enums.DependencyType;
import org.pfe.ppm_project.repositories.TaskDependencyRepository;
import org.pfe.ppm_project.repositories.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskDependencyService implements ITaskDependencyService {

    private final TaskDependencyRepository dependencyRepository;
    private final TaskRepository taskRepository;

    @Override
    public TaskDependencyDTO createDependency(TaskDependencyCreateRequest req) {

        if (req == null) throw new RuntimeException("request is required");
        if (req.predecessorTaskId() == null) throw new RuntimeException("predecessorTaskId is required");
        if (req.successorTaskId() == null) throw new RuntimeException("successorTaskId is required");

        if (req.predecessorTaskId().equals(req.successorTaskId())) {
            throw new RuntimeException("A task cannot depend on itself");
        }

        // Anti-duplicate (avant même de toucher la contrainte unique DB)
        if (dependencyRepository.existsByPredecessor_IdAndSuccessor_Id(req.predecessorTaskId(), req.successorTaskId())) {
            throw new RuntimeException("Dependency already exists");
        }

        // Recharger les tasks depuis la DB (sinon predecessor.project est null)
        Task predecessor = taskRepository.findById(req.predecessorTaskId())
                .orElseThrow(() -> new RuntimeException("Predecessor task not found: " + req.predecessorTaskId()));

        Task successor = taskRepository.findById(req.successorTaskId())
                .orElseThrow(() -> new RuntimeException("Successor task not found: " + req.successorTaskId()));

        // (Optionnel mais conseillé) ne pas lier des tâches inactives
        if (!predecessor.isActive() || !successor.isActive()) {
            throw new RuntimeException("Cannot create dependency with inactive tasks");
        }

        // Règle: même projet uniquement
        Long predProjectId = predecessor.getProject().getId();
        Long succProjectId = successor.getProject().getId();

        if (predProjectId == null || succProjectId == null || !predProjectId.equals(succProjectId)) {
            throw new RuntimeException("Dependency must be between tasks of the same project");
        }

        DependencyType type = (req.type() != null) ? req.type() : DependencyType.FS;

        TaskDependency dep = TaskDependency.builder()
                .predecessor(predecessor)
                .successor(successor)
                .type(type)
                .build();

        TaskDependency saved = dependencyRepository.save(dep);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDependencyDTO> getPredecessorsOfTask(Long taskId) {
        if (taskId == null) throw new RuntimeException("taskId is required");
        return dependencyRepository.findBySuccessor_Id(taskId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDependencyDTO> getSuccessorsOfTask(Long taskId) {
        if (taskId == null) throw new RuntimeException("taskId is required");
        return dependencyRepository.findByPredecessor_Id(taskId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public void deleteDependency(Long id) {
        if (id == null) throw new RuntimeException("id is required");
        dependencyRepository.deleteById(id);
    }

    private TaskDependencyDTO toDTO(TaskDependency d) {
        return new TaskDependencyDTO(
                d.getId(),
                d.getPredecessor() != null ? d.getPredecessor().getId() : null,
                d.getSuccessor() != null ? d.getSuccessor().getId() : null,
                d.getType(),
                d.getCreatedAt()
        );
    }
}
