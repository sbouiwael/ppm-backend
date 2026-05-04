package org.pfe.ppm_project.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pfe.ppm_project.entities.Project;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.repositories.ProjectRepository;
import org.pfe.ppm_project.repositories.TaskAssignmentRepository;
import org.pfe.ppm_project.repositories.TaskDependencyRepository;
import org.pfe.ppm_project.repositories.TaskRepository;
import org.pfe.ppm_project.services.IAuditLogService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour la propagation du progres dans TaskService.
 * Verifie que la progression d'un parent est la moyenne ponderee de ses enfants actifs.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceProgressTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ICalendarService calendarService;

    @Mock
    private IAuditLogService auditLogService;

    @Mock
    private TaskDependencyRepository taskDependencyRepository;

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(taskRepository, projectRepository, calendarService,
                auditLogService, taskDependencyRepository, taskAssignmentRepository);
    }

    @Test
    @DisplayName("Moyenne ponderee : enfant A (4h, 100%) + enfant B (4h, 0%) => parent = 50%")
    void weightedAverageProgress_equalWeights() {
        Project project = Project.builder().id(1L).build();

        Task parent = Task.builder().id(10L).name("Parent").project(project)
                .progress(0).workHours(8.0).active(true).build();
        Task childA = Task.builder().id(11L).name("Enfant A").project(project)
                .parentTask(parent).progress(100).workHours(4.0).active(true).build();
        Task childB = Task.builder().id(12L).name("Enfant B").project(project)
                .parentTask(parent).progress(0).workHours(4.0).active(true).build();

        // Setup update call
        Task updatedChildA = Task.builder().id(11L).name("Enfant A").project(project)
                .parentTask(parent).progress(100).workHours(4.0).active(true).build();

        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));
        // Parent's children
        when(taskRepository.findByParentTaskId(10L)).thenReturn(List.of(childA, childB));
        // Handles both updateTask(11L) lookup and propagation parent(10L) lookup
        when(taskRepository.findById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            if (id.equals(11L)) return Optional.of(childA);
            if (id.equals(10L)) return Optional.of(parent);
            return Optional.empty();
        });
        // projectRepository.findById not called — updatePayload has no startDate so
        // computeEndDateFromCalendar returns early

        // Simulate updateTask: parent.id is set and childA progress=100
        Task updatePayload = Task.builder()
                .id(null) // will be overwritten
                .name("Enfant A").project(project).parentTask(parent)
                .progress(100).workHours(4.0).active(true).durationDays(1.0)
                .wbsNumber("1").mode("TASK").sortOrder(0).status(
                        org.pfe.ppm_project.enums.TaskStatus.DONE)
                .build();

        service.updateTask(11L, updatePayload);

        // Capture the save call on the parent
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, atLeast(2)).save(captor.capture());

        // Find the parent save
        boolean parentSaved = captor.getAllValues().stream()
                .anyMatch(t -> t.getId() != null && t.getId().equals(10L) && t.getProgress() == 50);
        assertThat(parentSaved).isTrue();
    }

    @Test
    @DisplayName("Enfants inactifs sont ignores dans le calcul du progres parent")
    void inactiveChildren_areIgnoredInProgress() {
        Project project = Project.builder().id(1L).build();

        Task parent = Task.builder().id(20L).name("Parent").project(project)
                .progress(0).workHours(8.0).active(true).build();
        Task activeChild = Task.builder().id(21L).name("Actif").project(project)
                .parentTask(parent).progress(80).workHours(8.0).active(true).build();
        Task inactiveChild = Task.builder().id(22L).name("Inactif").project(project)
                .parentTask(parent).progress(0).workHours(8.0).active(false).build();

        when(taskRepository.findById(21L)).thenReturn(Optional.of(activeChild));
        when(taskRepository.findById(20L)).thenReturn(Optional.of(parent));
        when(taskRepository.findByParentTaskId(20L)).thenReturn(List.of(activeChild, inactiveChild));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));
        // projectRepository.findById not called — updatePayload has no startDate so
        // computeEndDateFromCalendar returns early

        Task updatePayload = Task.builder()
                .name("Actif").project(project).parentTask(parent)
                .progress(80).workHours(8.0).active(true).durationDays(1.0)
                .wbsNumber("1").mode("TASK").sortOrder(0)
                .status(org.pfe.ppm_project.enums.TaskStatus.IN_PROGRESS).build();

        service.updateTask(21L, updatePayload);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, atLeast(2)).save(captor.capture());

        // Parent progress should be 80 (only active child with 80% counts)
        boolean parentSavedAt80 = captor.getAllValues().stream()
                .anyMatch(t -> t.getId() != null && t.getId().equals(20L) && t.getProgress() == 80);
        assertThat(parentSavedAt80).isTrue();
    }
}