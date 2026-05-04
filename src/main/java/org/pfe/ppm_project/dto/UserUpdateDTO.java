package org.pfe.ppm_project.dto;

import jakarta.validation.constraints.*;
import org.pfe.ppm_project.enums.Role;

/**
 * DTO de mise à jour d'un utilisateur.
 *
 * Le mot de passe est optionnel : s'il est null ou vide, le mot de passe existant est conservé.
 * Si fourni, il doit respecter la longueur minimale et sera re-haché par UserService.
 *
 * Le champ active permet la désactivation manuelle (soft delete via PATCH sémantique sur PUT).
 * Le champ email ne peut pas être modifié pour un autre utilisateur existant
 * (unicité vérifiée par UserService / contrainte DB).
 */
public record UserUpdateDTO(

        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        /**
         * Optionnel. Si null ou vide : mot de passe conservé.
         * Si fourni : doit faire au moins 8 caractères. Re-haché par UserService.
         */
        @Size(min = 8, message = "Password must be at least 8 characters if provided")
        String password,

        @NotNull(message = "Role is required")
        Role role,

        @NotNull(message = "Weekly capacity is required")
        @Min(value = 0, message = "Weekly capacity cannot be negative")
        @Max(value = 80, message = "Weekly capacity cannot exceed 80 hours per week")
        Integer weeklyCapacity,

        boolean active

) {}