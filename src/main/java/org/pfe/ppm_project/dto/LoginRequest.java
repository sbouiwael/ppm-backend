package org.pfe.ppm_project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour la requete d'authentification.
 * Contient les identifiants de connexion (email + mot de passe).
 */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,       // Adresse email de l'utilisateur

        @NotBlank(message = "Password is required")
        String password     // Mot de passe en clair (sera verifie contre le hash BCrypt)
) {}
