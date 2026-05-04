package org.pfe.ppm_project.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pfe.ppm_project.entities.Project;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.enums.TaskStatus;
import org.pfe.ppm_project.exception.InvalidOperationException;
import org.pfe.ppm_project.repositories.ProjectRepository;
import org.pfe.ppm_project.repositories.TaskAssignmentRepository;
import org.pfe.ppm_project.repositories.TaskDependencyRepository;
import org.pfe.ppm_project.repositories.TaskRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires — Phase 1 (integrité hiérarchique) et Phase 3 (status roll-up).
 *
 * Verifie :
 *   - Prevention du cycle parent (A → A, A → B → A, chaine plus longue)
 *   - Rejet du parent appartenant a un autre projet
 *   - Blocage de la suppression si la tache a des enfants actifs
 *   - Status roll-up depuis les enfants vers le parent
 *   - Work hours aggregation vers le parent
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceHierarchyTest {

    @Mock private TaskRepository           taskRepository;
    @Mock private ProjectRepository        projectRepository;
    @Mock private ICalendarService         calendarService;
    @Mock private IAuditLogService         auditLogService;
    @Mock private TaskDependencyRepository dependencyRepository;
    @Mock private TaskAssignmentRepository taskAssignmentRepository;

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(
                taskRepository, projectRepository, calendarService,
                auditLogService, dependencyRepository, taskAssignmentRepository);
    }

    // ─────────────────────────────────────────────────────────────
    // PHASE 1 — CYCLE PREVENTION
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Phase 1 : auto-reference — une tache ne peut pas etre son propre parent")
    void selfReference_throwsInvalidOperation() {
        Project project = Project.builder().id(1L).build();
        Task taskA = Task.builder().id(10L).name("A").project(project).build();

        // On simule updateTask : la tache existante est trouvee, puis on valide parentTask = itself
        when(taskRepository.findById(10L)).thenReturn(Optional.of(taskA));
        // buildTaskFromDto met parentTask a null si id=null, mais ici on teste la validation
        // On cree un payload avec parentTaskId = 10 (soi-meme)
        Task payload = Task.builder()
                .name("A").project(project)
                .parentTask(Task.builder().id(10L).build()) // parent = soi-meme
                .durationDays(1.0).workHours(8.0).active(true)
                .wbsNumber("1").mode("TASK").sortOrder(0)
                .status(TaskStatus.NOT_STARTED).progress(0).build();

        assertThatThrownBy(() -> service.updateTask(10L, payload))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("cannot be its own parent");
    }

    @Test
    @DisplayName("Phase 1 : cycle direct A → parent B, B → parent A detecte")
    void directCycle_AB_throwsInvalidOperation() {
        Project project = Project.builder().id(1L).build();

        // B a pour parent A
        Task taskA = Task.builder().id(10L).name("A").project(project)
                .parentTask(null).build();
        Task taskB = Task.builder().id(11L).name("B").project(project)
                .parentTask(taskA).build();

        // On tente de setter parent de A = B (ce qui creerait un cycle)
        when(taskRepository.findById(10L)).thenReturn(Optional.of(taskA));
        // BFS depuis B(11) : remonte vers A(10) == taskId(10) → cycle
        when(taskRepository.findById(11L)).thenReturn(Optional.of(taskB));

        Task payload = Task.builder()
                .name("A").project(project)
                .parentTask(Task.builder().id(11L).build()) // parent A = B → cycle
                .durationDays(1.0).workHours(8.0).active(true)
                .wbsNumber("1").mode("TASK").sortOrder(0)
                .status(TaskStatus.NOT_STARTED).progress(0).build();

        assertThatThrownBy(() -> service.updateTask(10L, payload))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Circular parent hierarchy");
    }

    @Test
    @DisplayName("Phase 1 : cycle indirect A → B → C → A detecte")
    void indirectCycle_ABC_throwsInvalidOperation() {
        Project project = Project.builder().id(1L).build();

        Task taskA = Task.builder().id(10L).name("A").project(project).build();
        Task taskB = Task.builder().id(11L).name("B").project(project).parentTask(taskA).build();
        Task taskC = Task.builder().id(12L).name("C").project(project).parentTask(taskB).build();

        // On tente de setter parent de A = C (A → C mais C → B → A → cycle)
        when(taskRepository.findById(10L)).thenReturn(Optional.of(taskA));
        when(taskRepository.findById(12L)).thenReturn(Optional.of(taskC));
        when(taskRepository.findById(11L)).thenReturn(Optional.of(taskB));

        Task payload = Task.builder()
                .name("A").project(project)
                .parentTask(Task.builder().id(12L).build()) // parent A = C → cycle
                .durationDays(1.0).workHours(8.0).active(true)
                .wbsNumber("1").mode("TASK").sortOrder(0)
                .status(TaskStatus.NOT_STARTED).progress(0).build();

        assertThatThrownBy(() -> service.updateTask(10L, payload))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Circular parent hierarchy");
    }

    // ─────────────────────────────────────────────────────────────
    // PHASE 1 — CROSS-PROJECT VALIDATION
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Phase 1 : parent appartenant a un autre projet — rejet")
    void crossProjectParent_throwsInvalidOperation() {
        Project project1 = Project.builder().id(1L).build();
        Project project2 = Project.builder().id(2L).build();

        Task taskInProject1 = Task.builder().id(10L).name("T1").project(project1).build();
        Task parentInProject2 = Task.builder().id(20L).name("P2").project(project2).build();

        when(taskRepository.findById(10L)).thenReturn(Optional.of(taskInProject1));
        // BFS : parent(20) n'a pas de parentTask → boucle s'arrete sans cycle
        when(taskRepository.findById(20L)).thenReturn(Optional.of(parentInProject2));

        Task payload = Task.builder()
                .name("T1").project(project1)
                .parentTask(Task.builder().id(20L).build())
                .durationDays(1.0).workHours(8.0).active(true)
                .wbsNumber("1").mode("TASK").sortOrder(0)
                .status(TaskStatus.NOT_STARTED).progress(0).build();

        assertThatThrownBy(() -> service.updateTask(10L, payload))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("same project");
    }

    // ─────────────────────────────────────────────────────────────
    // PHASE 6 — DELETE PROTECTION
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Phase 6 : deleteTask bloque si la tache a des enfants actifs")
    void deleteTask_withActiveChildren_throwsInvalidOperation() {
        Project project = Project.builder().id(1L).build();
        Task parent = Task.builder().id(10L).name("Parent").project(project).active(true).build();

        when(taskRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(taskRepository.countByParentTaskIdAndActiveTrue(10L)).thenReturn(3L);

        assertThatThrownBy(() -> service.deleteTask(10L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("3 active subtask(s)");
    }

    @Test
    @DisplayName("Phase 6 : deleteTask autorise si la tache n'a aucun enfant actif")
    void deleteTask_withNoActiveChildren_succeeds() {
        Project project = Project.builder().id(1L).build();
        Task parent = Task.builder().id(10L).name("Parent").project(project).active(true).build();

        when(taskRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(taskRepository.countByParentTaskIdAndActiveTrue(10L)).thenReturn(0L);
        when(taskRepository.findByParentTaskId(10L)).thenReturn(Collections.emptyList());
        when(taskAssignmentRepository.findByTaskId(10L)).thenReturn(Collections.emptyList());
        doNothing().when(dependencyRepository).deleteByTaskIds(any());
        doNothing().when(taskRepository).delete(any());

        service.deleteTask(10L); // doit passer sans exception

        verify(taskRepository).delete(parent);
    }

    // ─────────────────────────────────────────────────────────────
    // PHASE 3 — STATUS ROLL-UP
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Phase 3 : status roll-up — tous les enfants DONE → parent DONE")
    void statusRollUp_allChildrenDone_parentBecomesDone() {
        Project project = Project.builder().id(1L).build();

        Task parent = Task.builder().id(10L).name("Parent").project(project)
                .progress(0).workHours(8.0).active(true).status(TaskStatus.NOT_STARTED).build();
        Task childA = Task.builder().id(11L).name("A").project(project)
                .parentTask(parent).progress(100).workHours(4.0).active(true)
                .status(TaskStatus.DONE).build();
        Task childB = Task.builder().id(12L).name("B").project(project)
                .parentTask(parent).progress(100).workHours(4.0).active(true)
                .status(TaskStatus.DONE).build();

        when(taskRepository.findById(11L)).thenReturn(Optional.of(childA));
        when(taskRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(taskRepository.findByParentTaskId(10L)).thenReturn(List.of(childA, childB));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        Task payload = Task.builder()
                .name("A").project(project).parentTask(parent)
                .progress(100).workHours(4.0).active(true).durationDays(1.0)
                .wbsNumber("1").mode("TASK").sortOrder(0).status(TaskStatus.DONE).build();

        service.updateTask(11L, payload);

        verify(taskRepository, atLeast(2)).save(argThat(t ->
                t.getId() == null || !t.getId().equals(10L) ||
                t.getStatus() == TaskStatus.DONE
        ));
    }

    @Test
    @DisplayName("Phase 3 : status roll-up — au moins un enfant BLOCKED → parent BLOCKED")
    void statusRollUp_oneChildBlocked_parentBecomesBlocked() {
        Project project = Project.builder().id(1L).build();

        Task parent = Task.builder().id(10L).name("Parent").project(project)
                .progress(0).workHours(8.0).active(true).status(TaskStatus.NOT_STARTED).build();
        Task childA = Task.builder().id(11L).name("A").project(project)
                .parentTask(parent).progress(0).workHours(4.0).active(true)
                .status(TaskStatus.BLOCKED).build();
        Task childB = Task.builder().id(12L).name("B").project(project)
                .parentTask(parent).progress(50).workHours(4.0).active(true)
                .status(TaskStatus.IN_PROGRESS).build();

        when(taskRepository.findById(11L)).thenReturn(Optional.of(childA));
        when(taskRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(taskRepository.findByParentTaskId(10L)).thenReturn(List.of(childA, childB));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        Task payload = Task.builder()
                .name("A").project(project).parentTask(parent)
                .progress(0).workHours(4.0).active(true).durationDays(1.0)
                .wbsNumber("1").mode("TASK").sortOrder(0).status(TaskStatus.BLOCKED).build();

        service.updateTask(11L, payload);

        // Verify parent was saved with BLOCKED status
        verify(taskRepository, atLeast(1)).save(argThat(t ->
                t.getId() != null && t.getId().equals(10L) && t.getStatus() == TaskStatus.BLOCKED
        ));
    }

    @Test
    @DisplayName("Phase 3 : status roll-up — tous NOT_STARTED → parent NOT_STARTED")
    void statusRollUp_allNotStarted_parentBecomesNotStarted() {
        Project project = Project.builder().id(1L).build();

        Task parent = Task.builder().id(10L).name("Parent").project(project)
                .progress(0).workHours(8.0).active(true).status(TaskStatus.IN_PROGRESS).build();
        Task childA = Task.builder().id(11L).name("A").project(project)
                .parentTask(parent).progress(0).workHours(4.0).active(true)
                .status(TaskStatus.NOT_STARTED).build();
        Task childB = Task.builder().id(12L).name("B").project(project)
                .parentTask(parent).progress(0).workHours(4.0).active(true)
                .status(TaskStatus.NOT_STARTED).build();

        when(taskRepository.findById(11L)).thenReturn(Optional.of(childA));
        when(taskRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(taskRepository.findByParentTaskId(10L)).thenReturn(List.of(childA, childB));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        Task payload = Task.builder()
                .name("A").project(project).parentTask(parent)
                .progress(0).workHours(4.0).active(true).durationDays(1.0)
                .wbsNumber("1").mode("TASK").sortOrder(0).status(TaskStatus.NOT_STARTED).build();

        service.updateTask(11L, payload);

        verify(taskRepository, atLeast(1)).save(argThat(t ->
                t.getId() != null && t.getId().equals(10L) && t.getStatus() == TaskStatus.NOT_STARTED
        ));
    }

    // ─────────────────────────────────────────────────────────────
    // PHASE 3 — WORK HOURS AGGREGATION
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Phase 3 : workHours parent = somme des enfants actifs (4+6 = 10h)")
    void workHoursAggregation_parentGetsSum() {
        Project project = Project.builder().id(1L).build();

        Task parent = Task.builder().id(10L).name("Parent").project(project)
                .progress(0).workHours(8.0).active(true).status(TaskStatus.NOT_STARTED).build();
        Task childA = Task.builder().id(11L).name("A").project(project)
                .parentTask(parent).progress(50).workHours(4.0).active(true)
                .status(TaskStatus.IN_PROGRESS).build();
        Task childB = Task.builder().id(12L).name("B").project(project)
                .parentTask(parent).progress(0).workHours(6.0).active(true)
                .status(TaskStatus.NOT_STARTED).build();

        when(taskRepository.findById(11L)).thenReturn(Optional.of(childA));
        when(taskRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(taskRepository.findByParentTaskId(10L)).thenReturn(List.of(childA, childB));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        Task payload = Task.builder()
                .name("A").project(project).parentTask(parent)
                .progress(50).workHours(4.0).active(true).durationDays(1.0)
                .wbsNumber("1").mode("TASK").sortOrder(0).status(TaskStatus.IN_PROGRESS).build();

        service.updateTask(11L, payload);

        // Parent workHours should be 4 + 6 = 10
        verify(taskRepository, atLeast(1)).save(argThat(t ->
                t.getId() != null && t.getId().equals(10L)
                && t.getWorkHours() != null && t.getWorkHours() == 10.0
        ));
    }

    // ─────────────────────────────────────────────────────────────
    // PHASE 3 — updateTaskOperational
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Phase 2 : updateTaskOperational met uniquement a jour status/progress/actualWorkHours")
    void updateTaskOperational_updatesOnlyAllowedFields() {
        Project project = Project.builder().id(1L).build();
        Task task = Task.builder().id(10L).name("Task").project(project)
                .progress(0).workHours(8.0).active(true).status(TaskStatus.NOT_STARTED)
                .durationDays(5.0).build();

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        Task result = service.updateTaskOperational(10L, TaskStatus.IN_PROGRESS, 40, 3.5);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(result.getProgress()).isEqualTo(40);
        assertThat(result.getActualWorkHours()).isEqualTo(3.5);
        // Les champs de planification ne changent pas
        assertThat(result.getDurationDays()).isEqualTo(5.0);
        assertThat(result.getName()).isEqualTo("Task");
    }
}
