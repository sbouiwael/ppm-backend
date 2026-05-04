package org.pfe.ppm_project.dto;

import java.util.List;

/**
 * DTO de lecture pour un portefeuille.
 * Inclut la liste des projets rattaches sous forme de ProjectDTO.
 */
public record PortefeuilleDTO(
        Long id,                    // Identifiant du portefeuille
        String nom,                 // Nom du portefeuille
        String description,         // Description
        List<ProjectDTO> projects   // Liste des projets contenus dans ce portefeuille
) {}
