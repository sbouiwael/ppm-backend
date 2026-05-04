package org.pfe.ppm_project.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pfe.ppm_project.dto.MyTaskDTO;
import org.pfe.ppm_project.entities.Project;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.entities.TaskAssignment;
import org.pfe.ppm_project.entities.User;
import org.pfe.ppm_project.enums.Role;
import org.pfe.ppm_project.enums.TaskStatus;
import org.pfe.ppm_project.repositories.TaskAssignmentRepository;
import org.pfe.ppm_project.repositories.TaskRepository;
import org.pfe.ppm_project.repositories.UserRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour la fonctionnalite "My Tasks" du service TaskAssignmentService.
 * Verifie que getMyTasks() retourne les donnees correctes, exclut les taches inactives,
 * et gere les cas limites (aucune affectation, statut null, dates nulles).
 *
 * Logique testee : les DEV/QA/DEVOPS voient uniquement leurs taches affectees actives.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MyTasks — TaskAssignmentService.getMyTasks()")
class MyTasksServiceTest {

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private IAuditLogService auditLogService;
    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private TaskAssignmentService service;

    private User devUser;
    private Project project;
    private Task activeTask;
    private TaskAssignment activeAssignment;

    @BeforeEach
    void setUp() {
        devUser = User.builder()
                .id(10L).firstName("Alice").lastName("Dev")
                .email("alice@company.com").role(Role.DEV)
                .weeklyCapacity(40).active(true).build();

        project = Project.builder()
                .id(5L).name("E-commerce Platform").active(true).build();

        activeTask = Task.builder()
                .id(1L).name("Implement login API")
                .status(TaskStatus.IN_PROGRESS).progress(30)
                .startDate(LocalDate.of(2024, 3, 1))
                .endDate(LocalDate.of(2024, 3, 8))
                .wbsNumber("1.1")
                .durationDays(5.0).workHours(40.0)
                .active(true)
                .project(project)
                .build();

        activeAssignment = TaskAssignment.builder()
                .id(100L).task(activeTask).user(devUser)
                .assignedHours(20).active(true).build();
    }

    @Test
    @DisplayName("getMyTasks() retourne les taches affectees a l'utilisateur")
    void getMyTasks_returnsAssignedTasks() {
        when(taskAssignmentRepository.findActiveAssignmentsByUserId(10L))
                .thenReturn(List.of(activeAssignment));

        List<MyTaskDTO> result = service.getMyTasks(10L);

        assertThat(result).hasSize(1);
        MyTaskDTO dto = result.get(0);
        assertThat(dto.assignmentId()).isEqualTo(100L);
        assertThat(dto.taskId()).isEqualTo(1L);
        assertThat(dto.taskName()).isEqualTo("Implement login API");
        assertThat(dto.taskStatus()).isEqualTo("IN_PROGRESS");
        assertThat(dto.taskProgress()).isEqualTo(30);
        assertThat(dto.startDate()).isEqualTo("2024-03-01");
        assertThat(dto.endDate()).isEqualTo("2024-03-08");
        assertThat(dto.wbsNumber()).isEqualTo("1.1");
        assertThat(dto.assignedHours()).isEqualTo(20);
        assertThat(dto.projectId()).isEqualTo(5L);
        assertThat(dto.projectName()).isEqualTo("E-commerce Platform");
        assertThat(dto.durationDays()).isEqualTo(5.0);
        assertThat(dto.workHours()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("getMyTasks() retourne liste vide si aucune affectation active")
    void getMyTasks_returnsEmptyListWhenNoAssignments() {
        when(taskAssignmentRepository.findActiveAssignmentsByUserId(10L))
                .thenReturn(List.of());

        List<MyTaskDTO> result = service.getMyTasks(10L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getMyTasks() gere le statut null — retourne NOT_STARTED par defaut")
    void getMyTasks_handlesNullStatus() {
        activeTask.setStatus(null);
        when(taskAssignmentRepository.findActiveAssignmentsByUserId(10L))
                .thenReturn(List.of(activeAssignment));

        List<MyTaskDTO> result = service.getMyTasks(10L);

        assertThat(result.get(0).taskStatus()).isEqualTo("NOT_STARTED");
    }

    @Test
    @DisplayName("getMyTasks() gere les dates nulles sans lever d'exception")
    void getMyTasks_handlesNullDates() {
        activeTask.setStartDate(null);
        activeTask.setEndDate(null);
        when(taskAssignmentRepository.findActiveAssignmentsByUserId(10L))
                .thenReturn(List.of(activeAssignment));

        List<MyTaskDTO> result = service.getMyTasks(10L);

        assertThat(result.get(0).startDate()).isNull();
        assertThat(result.get(0).endDate()).isNull();
    }

    @Test
    @DisplayName("getMyTasks() gere le progress null — retourne 0 par defaut")
    void getMyTasks_handlesNullProgress() {
        activeTask.setProgress(null);
        when(taskAssignmentRepository.findActiveAssignmentsByUserId(10L))
                .thenReturn(List.of(activeAssignment));

        List<MyTaskDTO> result = service.getMyTasks(10L);

        assertThat(result.get(0).taskProgress()).isEqualTo(0);
    }

    @Test
    @DisplayName("getMyTasks() retourne plusieurs taches issues de projets differents")
    void getMyTasks_returnsMultipleTasksFromDifferentProjects() {
        Project project2 = Project.builder().id(6L).name("Infrastructure Upgrade").active(true).build();
        Task task2 = Task.builder()
                .id(2L).name("Setup CI/CD")
                .status(TaskStatus.NOT_STARTED).progress(0)
                .wbsNumber("2.1").durationDays(3.0).workHours(24.0)
                .active(true).project(project2).build();
        TaskAssignment assignment2 = TaskAssignment.builder()
                .id(101L).task(task2).user(devUser)
                .assignedHours(24).active(true).build();

        when(taskAssignmentRepository.findActiveAssignmentsByUserId(10L))
                .thenReturn(List.of(activeAssignment, assignment2));

        List<MyTaskDTO> result = service.getMyTasks(10L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(MyTaskDTO::projectName)
                .containsExactlyInAnyOrder("E-commerce Platform", "Infrastructure Upgrade");
    }

    @Test
    @DisplayName("getMyTasks() utilise la query repository — ne fait pas de chargement lazy supplementaire")
    void getMyTasks_delegatesToRepository() {
        when(taskAssignmentRepository.findActiveAssignmentsByUserId(10L))
                .thenReturn(List.of());

        service.getMyTasks(10L);

        // Verifie que le repository est appele avec le bon userId
        verify(taskAssignmentRepository, times(1)).findActiveAssignmentsByUserId(10L);
        // Verifie qu'aucun autre repository n'est consulte (pas de N+1)
        verifyNoInteractions(taskRepository);
        verifyNoInteractions(userRepository);
    }
}
