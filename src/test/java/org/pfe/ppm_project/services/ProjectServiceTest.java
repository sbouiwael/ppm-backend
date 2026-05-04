package org.pfe.ppm_project.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pfe.ppm_project.dto.ProjectCreateUpdateDTO;
import org.pfe.ppm_project.dto.ProjectDTO;
import org.pfe.ppm_project.entities.Project;
import org.pfe.ppm_project.entities.User;
import org.pfe.ppm_project.enums.Role;
import org.pfe.ppm_project.repositories.ProjectRepository;
import org.pfe.ppm_project.repositories.TaskAssignmentRepository;
import org.pfe.ppm_project.repositories.TaskDependencyRepository;
import org.pfe.ppm_project.repositories.TaskRepository;
import org.pfe.ppm_project.repositories.UserRepository;
import org.pfe.ppm_project.repositories.WorkCalendarRepository;
import org.pfe.ppm_project.services.IAuditLogService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour ProjectService.
 * Vérifie la création, la désactivation (soft delete) et les getters.
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IFileStorageService fileStorageService;

    @Mock
    private WorkCalendarRepository workCalendarRepository;

    @Mock
    private IAuditLogService auditLogService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskDependencyRepository taskDependencyRepository;

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

    private ProjectService service;

    private User manager;

    @BeforeEach
    void setUp() {
        service  = new ProjectService(projectRepository, userRepository, fileStorageService,
                workCalendarRepository, auditLogService, taskRepository,
                taskDependencyRepository, taskAssignmentRepository);
        manager  = User.builder()
                .id(1L).firstName("Karim").lastName("Ben Hamida")
                .email("karim@biat.com.tn").role(Role.PM)
                .weeklyCapacity(40).active(true).build();
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: retourne Optional.empty() si le manager n'existe pas")
    void create_unknownManager_returnsEmpty() {
        ProjectCreateUpdateDTO dto = new ProjectCreateUpdateDTO(
                "Projet Test", "description",
                LocalDate.now(), LocalDate.now().plusMonths(6),
                true, 99L,     // ID manager inexistant
                null, null, null, null, null, null, null, 0, null
        );
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<ProjectDTO> result = service.create(dto);

        assertThat(result).isEmpty();
        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("create: projet créé avec le manager correct si DTO valide")
    void create_validDto_savesProject() {
        ProjectCreateUpdateDTO dto = new ProjectCreateUpdateDTO(
                "Core Banking T24", "Refonte du SI Core Banking",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                true, 1L,
                null, null, null, null, null, null, null, 0, null
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            // Simulate DB auto-increment
            return Project.builder()
                    .id(100L).name(p.getName()).description(p.getDescription())
                    .startDate(p.getStartDate()).endDate(p.getEndDate())
                    .active(true).progress(0).projectManager(manager)
                    .build();
        });

        Optional<ProjectDTO> result = service.create(dto);

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Core Banking T24");
        assertThat(result.get().projectManagerId()).isEqualTo(1L);
    }

    // ── deactivate ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivate: passe active=false et retourne true si projet trouvé")
    void deactivate_existingProject_returnsTrue() {
        Project existing = Project.builder()
                .id(1L).name("Mobile BIAT+").active(true).progress(58)
                .projectManager(manager)
                .startDate(LocalDate.now()).build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = service.deactivate(1L);

        assertThat(result).isTrue();
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    @DisplayName("deactivate: retourne false si projet inexistant")
    void deactivate_unknownProject_returnsFalse() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = service.deactivate(999L);

        assertThat(result).isFalse();
        verify(projectRepository, never()).save(any());
    }

    // ── getAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll: retourne la liste complète des projets")
    void getAll_returnsAllProjects() {
        Project p1 = Project.builder().id(1L).name("Projet A").active(true)
                .projectManager(manager).startDate(LocalDate.now()).progress(10).build();
        Project p2 = Project.builder().id(2L).name("Projet B").active(false)
                .projectManager(manager).startDate(LocalDate.now()).progress(90).build();

        when(projectRepository.findAll()).thenReturn(List.of(p1, p2));

        List<ProjectDTO> result = service.getAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProjectDTO::name).containsExactly("Projet A", "Projet B");
    }
}