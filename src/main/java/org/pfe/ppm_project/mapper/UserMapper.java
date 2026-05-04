package org.pfe.ppm_project.mapper;

import org.pfe.ppm_project.dto.UserDTO;
import org.pfe.ppm_project.entities.User;

/**
 * Mapper utilitaire pour convertir l'entite User en UserDTO.
 * Classe non instanciable (constructeur prive) avec methode statique.
 */
public final class UserMapper {
    private UserMapper() {}

    // Convertit un User en UserDTO (sans le mot de passe pour des raisons de securite)
    public static UserDTO toDTO(User u) {
        return new UserDTO(
                u.getId(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                u.getRole(),
                u.getWeeklyCapacity(),
                u.isActive(),
                u.getCreatedAt()
        );
    }
}
