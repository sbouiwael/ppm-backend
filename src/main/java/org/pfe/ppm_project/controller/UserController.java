package org.pfe.ppm_project.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.UserCreateDTO;
import org.pfe.ppm_project.dto.UserDTO;
import org.pfe.ppm_project.dto.UserUpdateDTO;
import org.pfe.ppm_project.entities.User;
import org.pfe.ppm_project.mapper.UserMapper;
import org.pfe.ppm_project.services.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller CRUD des utilisateurs.
 *
 * SECURITY FIX (2026-04): Les endpoints de création et de mise à jour acceptent
 * désormais des DTOs typés (UserCreateDTO / UserUpdateDTO) avec @Valid,
 * et non plus l'entité JPA brute. Cela empêche l'injection de champs
 * sensibles (role, active, password en clair) par un appelant malveillant.
 *
 * La conversion DTO → entité est effectuée dans ce controller.
 * Le hashage BCrypt du mot de passe reste dans UserService.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    // ── Lecture ──────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RH','PMO','PM')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> list = userService.getAllUsers().stream()
                .map(UserMapper::toDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RH','PMO','PM')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(UserMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyRole('ADMIN','RH')")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(UserMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Écriture — réservée ADMIN et RH ──────────────────────────────────────

    /**
     * Création d'un utilisateur à partir d'un DTO validé.
     * Le mot de passe en clair est haché par UserService avant persistance.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RH')")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserCreateDTO dto) {
        User toCreate = User.builder()
                .firstName(dto.firstName().strip())
                .lastName(dto.lastName().strip())
                .email(dto.email().strip().toLowerCase())
                .password(dto.password())           // haché par UserService
                .role(dto.role())
                .weeklyCapacity(dto.weeklyCapacity())
                .active(true)                       // toujours actif à la création
                .build();

        User created = userService.createUser(toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toDTO(created));
    }

    /**
     * Mise à jour d'un utilisateur existant.
     * Si password est null/vide dans le DTO, le mot de passe actuel est conservé.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RH')")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id,
                                              @Valid @RequestBody UserUpdateDTO dto) {
        User toUpdate = User.builder()
                .firstName(dto.firstName().strip())
                .lastName(dto.lastName().strip())
                .email(dto.email().strip().toLowerCase())
                .password(dto.password())           // null/vide = conserve l'existant (UserService gère)
                .role(dto.role())
                .weeklyCapacity(dto.weeklyCapacity())
                .active(dto.active())
                .build();

        return ResponseEntity.ok(UserMapper.toDTO(userService.updateUser(id, toUpdate)));
    }

    /**
     * Active ou desactive un utilisateur sans modifier ses autres donnees.
     */
    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('ADMIN','RH')")
    public ResponseEntity<Void> setUserActive(@PathVariable Long id,
                                               @RequestParam boolean active) {
        userService.setUserActive(id, active);
        return ResponseEntity.noContent().build();
    }

    /**
     * Suppression physique d'un utilisateur.
     * Supprime d'abord les affectations et notifications liées avant de supprimer l'utilisateur.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RH')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}