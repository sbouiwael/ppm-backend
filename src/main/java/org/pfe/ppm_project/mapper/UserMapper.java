package org.pfe.ppm_project.mapper;

import org.pfe.ppm_project.dto.UserDTO;
import org.pfe.ppm_project.entities.User;

public final class UserMapper {
    private UserMapper() {}

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
