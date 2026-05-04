package org.pfe.ppm_project.dto;

import org.pfe.ppm_project.enums.Role;

/**
 * DTO de reponse apres une authentification reussie.
 * Retourne le token JWT et les informations essentielles de l'utilisateur.
 */
public record LoginResponse(
        String token,       // Token JWT pour les requetes authentifiees
        Long userId,        // Identifiant de l'utilisateur connecte
        String email,       // Adresse email
        String firstName,   // Prenom
        String lastName,    // Nom de famille
        Role role           // Role de l'utilisateur
) {}
