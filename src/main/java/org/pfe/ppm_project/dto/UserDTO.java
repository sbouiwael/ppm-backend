package org.pfe.ppm_project.dto;

import org.pfe.ppm_project.enums.Role;

import java.time.LocalDateTime;

public record UserDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        Integer weeklyCapacity,
        boolean active,
        LocalDateTime createdAt
) {}
