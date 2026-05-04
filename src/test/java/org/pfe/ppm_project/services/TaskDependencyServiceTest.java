package org.pfe.ppm_project.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pfe.ppm_project.dto.TaskDependencyCreateRequest;
import org.pfe.ppm_project.entities.Project;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.entities.TaskDependency;
import org.pfe.ppm_project.enums.DependencyType;
import org.pfe.ppm_project.exception.InvalidOperationException;
import org.pfe.ppm_project.repositories.TaskDependencyRepository;
import org.pfe.ppm_project.repositories.TaskRepository;
import org.pfe.ppm_project.services.IAuditLogService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour TaskDependencyService.
 * Verifie les regles metier : auto-dependance, doublon, cycle, meme projet.
 */
@ExtendWith(MockitoExtension.class)
class TaskDependencyServiceTest {

    @Mock
    private TaskDependencyRepository dependencyRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private IAuditLogService auditLogService;

    private TaskDependencyService service;

    private Project projectA;
    private Task taskA, taskB, taskC;

    @BeforeEach
    void setUp() {
        service = new TaskDependencyService(dependencyRepository, taskRepository, auditLogService);

        projectA = Project.builder().id(1L).name("Projet A").build();

        taskA = Task.builder().id(1L).name("Tache A").project(projectA).active(true).build();
        taskB = Task.builder().id(2L).name("Tache B").project(projectA).active(true).build();
        taskC = Task.builder().id(3L).name("Tache C").project(projectA).active(true).build();
    }

    @Test
    @DisplayName("Auto-dependance : une tache ne peut pas dependre d'elle-meme")
    void selfDependency_throwsException() {
        TaskDependencyCreateRequest req = new TaskDependencyCreateRequest(1L, 1L, DependencyType.FS);

        assertThatThrownBy(() -> service.createDependency(req))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("cannot depend on itself");
    }

    @Test
    @DisplayName("Doublon : ne peut pas creer la meme dependance deux fois")
    void duplicateDependency_throwsException() {
        TaskDependencyCreateRequest req = new TaskDependencyCreateRequest(1L, 2L, DependencyType.FS);

        when(dependencyRepository.existsByPredecessor_IdAndSuccessor_Id(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.createDependency(req))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Cycle : A->B->C ne peut pas creer C->A")
    void cycleDependency_throwsException() {
        // Setup: A->B->C exists, trying to add C->A (would create cycle)
        TaskDependencyCreateRequest req = new TaskDependencyCreateRequest(3L, 1L, DependencyType.FS);

        when(dependencyRepository.existsByPredecessor_IdAndSuccessor_Id(3L, 1L)).thenReturn(false);

        // BFS from successorId=1: find successors of A (=B), then successors of B (=C)
        // When checking cycle: start from successorId=1, traverse forward:
        // successors of 1 = [B(2)], successors of 2 = [C(3)], C==predecessorId(3) -> cycle
        TaskDependency aToB = buildDep(10L, taskA, taskB);
        TaskDependency bToC = buildDep(11L, taskB, taskC);

        when(dependencyRepository.findByPredecessor_Id(1L)).thenReturn(List.of(aToB));
        when(dependencyRepository.findByPredecessor_Id(2L)).thenReturn(List.of(bToC));
        // BFS returns true when pop(3)==predecessorId(3), so findByPredecessor_Id(3) is never called

        assertThatThrownBy(() -> service.createDependency(req))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Circular dependency");
    }

    @Test
    @DisplayName("Tache inactive : creation de dependance interdite")
    void inactiveTask_throwsException() {
        Task inactiveTask = Task.builder().id(4L).name("Inactive").project(projectA).active(false).build();

        TaskDependencyCreateRequest req = new TaskDependencyCreateRequest(1L, 4L, DependencyType.FS);

        when(dependencyRepository.existsByPredecessor_IdAndSuccessor_Id(1L, 4L)).thenReturn(false);
        // BFS starts from successorId=4, so first call is findByPredecessor_Id(4L)
        when(dependencyRepository.findByPredecessor_Id(4L)).thenReturn(List.of());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskA));
        when(taskRepository.findById(4L)).thenReturn(Optional.of(inactiveTask));

        assertThatThrownBy(() -> service.createDependency(req))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("Projets differents : dependance inter-projet interdite")
    void crossProjectDependency_throwsException() {
        Project projectB = Project.builder().id(2L).name("Projet B").build();
        Task taskOtherProject = Task.builder().id(5L).name("Autre Projet").project(projectB).active(true).build();

        TaskDependencyCreateRequest req = new TaskDependencyCreateRequest(1L, 5L, DependencyType.FS);

        when(dependencyRepository.existsByPredecessor_IdAndSuccessor_Id(1L, 5L)).thenReturn(false);
        // BFS starts from successorId=5, so first call is findByPredecessor_Id(5L)
        when(dependencyRepository.findByPredecessor_Id(5L)).thenReturn(List.of());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskA));
        when(taskRepository.findById(5L)).thenReturn(Optional.of(taskOtherProject));

        assertThatThrownBy(() -> service.createDependency(req))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("same project");
    }

    @Test
    @DisplayName("Dependance valide A->B se cree correctement avec le type FS par defaut")
    void validDependency_createdSuccessfully() {
        TaskDependencyCreateRequest req = new TaskDependencyCreateRequest(1L, 2L, null); // null type -> defaults to FS

        when(dependencyRepository.existsByPredecessor_IdAndSuccessor_Id(1L, 2L)).thenReturn(false);
        // BFS starts from successorId=2, so first call is findByPredecessor_Id(2L)
        when(dependencyRepository.findByPredecessor_Id(2L)).thenReturn(List.of()); // no successors of B
        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskA));
        when(taskRepository.findById(2L)).thenReturn(Optional.of(taskB));

        TaskDependency saved = buildDep(99L, taskA, taskB);
        when(dependencyRepository.save(any())).thenReturn(saved);

        var result = service.createDependency(req);

        verify(dependencyRepository, times(1)).save(any());
        assert result.predecessorTaskId().equals(1L);
        assert result.successorTaskId().equals(2L);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private TaskDependency buildDep(Long id, Task pred, Task succ) {
        return TaskDependency.builder()
                .id(id)
                .predecessor(pred)
                .successor(succ)
                .type(DependencyType.FS)
                .createdAt(LocalDateTime.now())
                .build();
    }
}