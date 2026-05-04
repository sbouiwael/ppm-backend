package org.pfe.ppm_project.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pfe.ppm_project.dto.ResourceCapacityDTO;
import org.pfe.ppm_project.entities.Project;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.entities.TaskAssignment;
import org.pfe.ppm_project.entities.User;
import org.pfe.ppm_project.enums.Role;
import org.pfe.ppm_project.repositories.TaskAssignmentRepository;
import org.pfe.ppm_project.repositories.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour CapacityService.getCapacityOverview().
 *
 * Verifie :
 *   - Calcul du taux d'utilisation (utilizationPct)
 *   - Classification du statut (OVERLOADED / BALANCED / UNDERUTILIZED / NO_CAPACITY)
 *   - Filtrage par role
 *   - Filtrage par projectId
 *   - Exclusion des utilisateurs inactifs
 *   - Gestion de weeklyCapacity = 0 (NO_CAPACITY)
 *   - Tri par taux d'utilisation decroissant
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CapacityService — getCapacityOverview()")
class CapacityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

    @InjectMocks
    private CapacityService capacityService;

    private User devUser;
    private User qaUser;
    private User inactiveUser;
    private Project project1;
    private Task task1;
    private TaskAssignment assignment1;

    @BeforeEach
    void setUp() {
        devUser = User.builder()
                .id(1L).firstName("Alice").lastName("Dev")
                .email("alice@biat.tn").role(Role.DEV)
                .weeklyCapacity(40).active(true).build();

        qaUser = User.builder()
                .id(2L).firstName("Bob").lastName("QA")
                .email("bob@biat.tn").role(Role.QA)
                .weeklyCapacity(40).active(true).build();

        inactiveUser = User.builder()
                .id(3L).firstName("Inactive").lastName("User")
                .email("inactive@biat.tn").role(Role.DEV)
                .weeklyCapacity(40).active(false).build();

        project1 = Project.builder().id(10L).name("Core Banking T24").active(true).build();

        task1 = Task.builder()
                .id(100L).name("API Integration")
                .active(true).project(project1).build();

        assignment1 = TaskAssignment.builder()
                .id(1000L).task(task1).user(devUser)
                .assignedHours(50).active(true).build();
    }

    @Test
    @DisplayName("retourne les utilisateurs actifs avec leur charge correctement calculee")
    void getCapacityOverview_returnsActiveUsersWithCorrectWorkload() {
        when(userRepository.findAll()).thenReturn(List.of(devUser, qaUser));
        when(taskAssignmentRepository.findByUserIdWithRefs(1L)).thenReturn(List.of(assignment1));
        when(taskAssignmentRepository.findByUserIdWithRefs(2L)).thenReturn(List.of());

        List<ResourceCapacityDTO> result = capacityService.getCapacityOverview(null, null);

        assertThat(result).hasSize(2);
        // devUser avec 50h assignees sur 40h capacite → OVERLOADED, 125%
        ResourceCapacityDTO devDto = result.stream()
                .filter(r -> r.userId().equals(1L)).findFirst().orElseThrow();
        assertThat(devDto.totalAssignedHours()).isEqualTo(50);
        assertThat(devDto.weeklyCapacity()).isEqualTo(40);
        assertThat(devDto.utilizationPct()).isEqualTo(125.0);
        assertThat(devDto.capacityStatus()).isEqualTo("OVERLOADED");
    }

    @Test
    @DisplayName("utilisateur sans affectation → UNDERUTILIZED (0%)")
    void getCapacityOverview_userWithNoAssignments_isUnderutilized() {
        when(userRepository.findAll()).thenReturn(List.of(qaUser));
        when(taskAssignmentRepository.findByUserIdWithRefs(2L)).thenReturn(List.of());

        List<ResourceCapacityDTO> result = capacityService.getCapacityOverview(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).capacityStatus()).isEqualTo("UNDERUTILIZED");
        assertThat(result.get(0).utilizationPct()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("utilisateur avec weeklyCapacity=0 → NO_CAPACITY")
    void getCapacityOverview_userWithZeroCapacity_isNoCapacity() {
        devUser.setWeeklyCapacity(0);
        when(userRepository.findAll()).thenReturn(List.of(devUser));
        when(taskAssignmentRepository.findByUserIdWithRefs(1L)).thenReturn(List.of(assignment1));

        List<ResourceCapacityDTO> result = capacityService.getCapacityOverview(null, null);

        assertThat(result.get(0).capacityStatus()).isEqualTo("NO_CAPACITY");
        assertThat(result.get(0).utilizationPct()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("charge a 80% → BALANCED")
    void getCapacityOverview_at80Percent_isBalanced() {
        TaskAssignment assign32h = TaskAssignment.builder()
                .id(999L).task(task1).user(devUser)
                .assignedHours(32).active(true).build(); // 32/40 = 80%
        when(userRepository.findAll()).thenReturn(List.of(devUser));
        when(taskAssignmentRepository.findByUserIdWithRefs(1L)).thenReturn(List.of(assign32h));

        List<ResourceCapacityDTO> result = capacityService.getCapacityOverview(null, null);

        assertThat(result.get(0).capacityStatus()).isEqualTo("BALANCED");
        assertThat(result.get(0).utilizationPct()).isEqualTo(80.0);
    }

    @Test
    @DisplayName("filtre par role — retourne uniquement les utilisateurs du role donne")
    void getCapacityOverview_filterByRole_returnsOnlyMatchingRole() {
        when(userRepository.findAll()).thenReturn(List.of(devUser, qaUser));
        when(taskAssignmentRepository.findByUserIdWithRefs(2L)).thenReturn(List.of());

        List<ResourceCapacityDTO> result = capacityService.getCapacityOverview("QA", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).role()).isEqualTo("QA");
    }

    @Test
    @DisplayName("filtre par projectId — ignore les affectations hors projet")
    void getCapacityOverview_filterByProject_countsOnlyProjectHours() {
        Project project2 = Project.builder().id(20L).name("Other Project").active(true).build();
        Task taskOtherProject = Task.builder()
                .id(200L).name("Task other project")
                .active(true).project(project2).build();
        TaskAssignment assignOtherProject = TaskAssignment.builder()
                .id(2000L).task(taskOtherProject).user(devUser)
                .assignedHours(30).active(true).build();

        // devUser a 50h sur project1 + 30h sur project2
        when(userRepository.findAll()).thenReturn(List.of(devUser));
        when(taskAssignmentRepository.findByUserIdWithRefs(1L))
                .thenReturn(List.of(assignment1, assignOtherProject));

        // En filtrant par project1 uniquement : doit voir 50h, pas 80h
        List<ResourceCapacityDTO> result = capacityService.getCapacityOverview(null, 10L);

        assertThat(result.get(0).totalAssignedHours()).isEqualTo(50);
        assertThat(result.get(0).utilizationPct()).isEqualTo(125.0);
    }

    @Test
    @DisplayName("exclut les utilisateurs inactifs")
    void getCapacityOverview_excludesInactiveUsers() {
        when(userRepository.findAll()).thenReturn(List.of(devUser, inactiveUser));
        when(taskAssignmentRepository.findByUserIdWithRefs(1L)).thenReturn(List.of());

        List<ResourceCapacityDTO> result = capacityService.getCapacityOverview(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("resultat trie par taux d'utilisation decroissant")
    void getCapacityOverview_isSortedByUtilizationDesc() {
        // devUser 50/40 = 125%, qaUser 0/40 = 0%
        when(userRepository.findAll()).thenReturn(List.of(qaUser, devUser)); // ordre inverse intentionnel
        when(taskAssignmentRepository.findByUserIdWithRefs(1L)).thenReturn(List.of(assignment1));
        when(taskAssignmentRepository.findByUserIdWithRefs(2L)).thenReturn(List.of());

        List<ResourceCapacityDTO> result = capacityService.getCapacityOverview(null, null);

        assertThat(result.get(0).userId()).isEqualTo(1L); // devUser (125%) en premier
        assertThat(result.get(1).userId()).isEqualTo(2L); // qaUser (0%) en second
    }

    @Test
    @DisplayName("exclut les affectations inactives du calcul")
    void getCapacityOverview_excludesInactiveAssignments() {
        TaskAssignment inactiveAssign = TaskAssignment.builder()
                .id(9999L).task(task1).user(devUser)
                .assignedHours(100).active(false).build(); // inactive — ne doit pas etre comptee

        when(userRepository.findAll()).thenReturn(List.of(devUser));
        when(taskAssignmentRepository.findByUserIdWithRefs(1L))
                .thenReturn(List.of(assignment1, inactiveAssign));

        List<ResourceCapacityDTO> result = capacityService.getCapacityOverview(null, null);

        // Doit compter uniquement les 50h de assignment1, pas les 100h de inactiveAssign
        assertThat(result.get(0).totalAssignedHours()).isEqualTo(50);
    }
}