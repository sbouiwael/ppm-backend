package org.pfe.ppm_project.dto;

import jakarta.validation.constraints.*;
import org.pfe.ppm_project.enums.Role;

/**
 * DTO de création d'un utilisateur.
 *
 * Remplace l'ancienne approche qui acceptait l'entité User brute dans le controller,
 * exposant des champs sensibles (active, createdAt, password en clair dans les logs).
 *
 * Seuls les champs métier nécessaires à la création sont acceptés.
 * Le champ active est toujours true à la création (géré par UserService).
 * Le mot de passe est hashé en BCrypt par UserService avant la persistance.
 */
public record UserCreateDTO(

        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotNull(message = "Role is required")
        Role role,

        @NotNull(message = "Weekly capacity is required")
        @Min(value = 0, message = "Weekly capacity cannot be negative")
        @Max(value = 80, message = "Weekly capacity cannot exceed 80 hours per week")
        Integer weeklyCapacity

) {}