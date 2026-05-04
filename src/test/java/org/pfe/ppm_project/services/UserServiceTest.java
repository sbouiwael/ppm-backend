package org.pfe.ppm_project.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pfe.ppm_project.entities.User;
import org.pfe.ppm_project.enums.Role;
import org.pfe.ppm_project.exception.ResourceNotFoundException;
import org.pfe.ppm_project.repositories.NotificationRepository;
import org.pfe.ppm_project.repositories.ProjectRepository;
import org.pfe.ppm_project.repositories.TaskAssignmentRepository;
import org.pfe.ppm_project.repositories.UserRepository;
import org.pfe.ppm_project.services.IAuditLogService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour UserService.
 * Vérifie le hashage BCrypt, la mise à jour partielle du mot de passe,
 * et la désactivation logique.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private IAuditLogService auditLogService;

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ProjectRepository projectRepository;

    private PasswordEncoder passwordEncoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new UserService(userRepository, passwordEncoder, auditLogService,
                taskAssignmentRepository, notificationRepository, projectRepository);
    }

    // ── createUser ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createUser: le mot de passe est haché en BCrypt avant persistance")
    void createUser_passwordIsHashed() {
        String rawPassword = "Biat@2026!";
        User toCreate = User.builder()
                .firstName("Ali").lastName("Ben Salah")
                .email("ali@biat.com.tn").password(rawPassword)
                .role(Role.DEV).weeklyCapacity(40).active(true)
                .build();

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createUser(toCreate);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getPassword()).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("createUser: le mot de passe hashé commence par $2a$ ou $2b$ (BCrypt)")
    void createUser_passwordStartsWithBcryptPrefix() {
        User toCreate = User.builder()
                .firstName("Sara").lastName("Mrad")
                .email("sara@biat.com.tn").password("SecurePass1!")
                .role(Role.QA).weeklyCapacity(35).active(true)
                .build();

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        service.createUser(toCreate);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        String hashed = captor.getValue().getPassword();
        assertThat(hashed).matches("\\$2[ab]\\$.*");
    }

    // ── updateUser ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateUser: mot de passe null dans le DTO → mot de passe existant conservé")
    void updateUser_nullPassword_keepsExisting() {
        String existingHash = passwordEncoder.encode("OldPassword!");
        User existing = User.builder()
                .id(1L).firstName("Omar").lastName("Jlassi")
                .email("omar@biat.com.tn").password(existingHash)
                .role(Role.DEV).weeklyCapacity(40).active(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User updatePayload = User.builder()
                .firstName("Omar").lastName("Jlassi")
                .email("omar@biat.com.tn")
                .password(null)           // null → conserve l'existant
                .role(Role.DEV).weeklyCapacity(40).active(true)
                .build();

        User result = service.updateUser(1L, updatePayload);

        assertThat(result.getPassword()).isEqualTo(existingHash);
    }

    @Test
    @DisplayName("updateUser: nouveau mot de passe fourni → re-haché en BCrypt")
    void updateUser_newPassword_isHashed() {
        String existingHash = passwordEncoder.encode("OldPassword!");
        User existing = User.builder()
                .id(2L).firstName("Nour").lastName("Chaabane")
                .email("nour@biat.com.tn").password(existingHash)
                .role(Role.PM).weeklyCapacity(40).active(true)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User updatePayload = User.builder()
                .firstName("Nour").lastName("Chaabane")
                .email("nour@biat.com.tn")
                .password("NewPass@2026!")   // nouveau mot de passe en clair
                .role(Role.PM).weeklyCapacity(40).active(true)
                .build();

        User result = service.updateUser(2L, updatePayload);

        assertThat(result.getPassword()).isNotEqualTo(existingHash);
        assertThat(passwordEncoder.matches("NewPass@2026!", result.getPassword())).isTrue();
    }

    @Test
    @DisplayName("updateUser: password déjà hashé ($2a$) → non re-encodé (évite double hash)")
    void updateUser_alreadyHashedPassword_notReEncoded() {
        String bcryptHash = passwordEncoder.encode("SomePass!");
        User existing = User.builder()
                .id(3L).firstName("Med").lastName("Karray")
                .email("med@biat.com.tn").password(bcryptHash)
                .role(Role.DEVOPS).weeklyCapacity(40).active(true)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User updatePayload = User.builder()
                .firstName("Med").lastName("Karray")
                .email("med@biat.com.tn")
                .password(bcryptHash)   // déjà haché — ne doit pas être re-encodé
                .role(Role.DEVOPS).weeklyCapacity(40).active(true)
                .build();

        User result = service.updateUser(3L, updatePayload);
        assertThat(result.getPassword()).isEqualTo(bcryptHash);
    }

    // ── deactivateUser ──────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivateUser: passe active=false sur l'utilisateur trouvé")
    void deactivateUser_setsActiveFalse() {
        User active = User.builder()
                .id(10L).firstName("Rim").lastName("Ben Ali")
                .email("rim@biat.com.tn").password("hash")
                .role(Role.RH).weeklyCapacity(35).active(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(active));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deactivateUser(10L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    @DisplayName("deactivateUser: lève ResourceNotFoundException si utilisateur inconnu")
    void deactivateUser_unknownId_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deactivateUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateUser: lève ResourceNotFoundException si utilisateur inconnu")
    void updateUser_unknownId_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateUser(99L, User.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}