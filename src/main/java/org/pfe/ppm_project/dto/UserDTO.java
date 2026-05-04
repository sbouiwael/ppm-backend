package org.pfe.ppm_project.dto;

import org.pfe.ppm_project.enums.Role;

import java.time.LocalDateTime;

/**
 * DTO de lecture pour un utilisateur.
 * Transfere les informations publiques de l'utilisateur (sans le mot de passe).
 */
public record UserDTO(
        Long id,                    // Identifiant de l'utilisateur
        String firstName,           // Prenom
        String lastName,            // Nom de famille
        String email,               // Adresse email
        Role role,                  // Role dans le systeme
        Integer weeklyCapacity,     // Capacite hebdomadaire en heures
        boolean active,             // Compte actif ou desactive
        LocalDateTime createdAt     // Date de creation du compte
) {}
