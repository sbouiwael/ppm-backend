package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.entities.User;
import org.pfe.ppm_project.enums.AuditAction;
import org.pfe.ppm_project.enums.Role;
import org.pfe.ppm_project.exception.InvalidOperationException;
import org.pfe.ppm_project.repositories.NotificationRepository;
import org.pfe.ppm_project.repositories.ProjectRepository;
import org.pfe.ppm_project.repositories.TaskAssignmentRepository;
import org.pfe.ppm_project.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service metier pour la gestion des utilisateurs.
 * Gere le CRUD des utilisateurs avec hashage securise des mots de passe (BCrypt).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IAuditLogService auditLogService;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final NotificationRepository notificationRepository;
    private final ProjectRepository projectRepository;

    // Cree un nouvel utilisateur en hashant son mot de passe avant sauvegarde
    @Override
    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        auditLogService.log(
                AuditAction.CREATE, "USER", saved.getId(),
                saved.getFirstName() + " " + saved.getLastName(),
                "User created with role " + saved.getRole(),
                null, null
        );
        return saved;
    }

    // Retourne la liste de tous les utilisateurs
    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Retourne un utilisateur par son identifiant
    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Retourne un utilisateur par son email (utilise pour l'authentification)
    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Met a jour un utilisateur existant. Le mot de passe n'est re-hashe que s'il est fourni et non vide.
    @Override
    public User updateUser(Long id, User updatedUser) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setFirstName(updatedUser.getFirstName());
                    user.setLastName(updatedUser.getLastName());
                    user.setEmail(updatedUser.getEmail());
                    user.setRole(updatedUser.getRole());
                    user.setWeeklyCapacity(updatedUser.getWeeklyCapacity());
                    user.setActive(updatedUser.isActive());

                    // Gestion du mot de passe : re-hashe uniquement si un nouveau mot de passe est fourni
                    if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
                        // Evite de re-encoder un hash BCrypt deja existant
                        if (!updatedUser.getPassword().startsWith("$2a$") && !updatedUser.getPassword().startsWith("$2b$")) {
                            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                        } else {
                            user.setPassword(updatedUser.getPassword());
                        }
                    }
                    // Si le mot de passe est null/vide, on conserve le mot de passe existant

                    User saved = userRepository.save(user);
                    auditLogService.log(
                            AuditAction.UPDATE, "USER", saved.getId(),
                            saved.getFirstName() + " " + saved.getLastName(),
                            "User updated | role=" + saved.getRole() + " active=" + saved.isActive(),
                            null, null
                    );
                    return saved;
                })
                .orElseThrow(() -> new org.pfe.ppm_project.exception.ResourceNotFoundException("User", id));
    }

    // Desactive un utilisateur (suppression logique)
    @Override
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new org.pfe.ppm_project.exception.ResourceNotFoundException("User", id));

        // Protection hierarchie des roles : un non-ADMIN ne peut pas desactiver un ADMIN
        if (user.getRole() == Role.ADMIN) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean callerIsAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!callerIsAdmin) {
                throw new InvalidOperationException("Cannot deactivate an ADMIN user");
            }
        }

        user.setActive(false);
        userRepository.save(user);
        auditLogService.log(
                AuditAction.DELETE, "USER", user.getId(),
                user.getFirstName() + " " + user.getLastName(),
                "User deactivated (soft delete)",
                null, null
        );
    }

    // Active ou desactive un utilisateur sans modifier ses autres donnees
    @Override
    public void setUserActive(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new org.pfe.ppm_project.exception.ResourceNotFoundException("User", id));

        if (user.getRole() == Role.ADMIN && !active) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean callerIsAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!callerIsAdmin) {
                throw new InvalidOperationException("Cannot deactivate an ADMIN user");
            }
        }

        user.setActive(active);
        userRepository.save(user);
        auditLogService.log(
                AuditAction.UPDATE, "USER", user.getId(),
                user.getFirstName() + " " + user.getLastName(),
                active ? "User activated" : "User deactivated",
                null, null
        );
    }

    // Suppression physique d'un utilisateur avec nettoyage des dependances
    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new org.pfe.ppm_project.exception.ResourceNotFoundException("User", id));

        if (user.getRole() == Role.ADMIN) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean callerIsAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!callerIsAdmin) {
                throw new InvalidOperationException("Cannot delete an ADMIN user");
            }
        }

        String fullName = user.getFirstName() + " " + user.getLastName();

        // Detache l'utilisateur des projets qu'il gere (FK nullable)
        projectRepository.clearProjectManagerByUserId(id);

        // Supprime les enregistrements dependants pour eviter les violations de contrainte FK
        taskAssignmentRepository.deleteAll(taskAssignmentRepository.findByUserId(id));
        notificationRepository.deleteAll(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(id));

        userRepository.delete(user);

        auditLogService.log(
                AuditAction.DELETE, "USER", id,
                fullName,
                "User permanently deleted",
                null, null
        );
    }
}
