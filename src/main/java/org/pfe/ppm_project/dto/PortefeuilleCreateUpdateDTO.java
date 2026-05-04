package org.pfe.ppm_project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO pour la creation et la mise a jour d'un portefeuille.
 * Contient le nom (obligatoire) et la description avec leurs contraintes de validation.
 */
public record PortefeuilleCreateUpdateDTO(
        @NotBlank(message = "Portfolio name is required")
        @Size(min = 2, max = 255, message = "Name must be 2-255 characters")
        String nom,             // Nom du portefeuille (obligatoire, 2-255 caracteres)

        @Size(max = 2000, message = "Description max 2000 characters")
        String description      // Description (optionnelle, max 2000 caracteres)
) {}
