package org.pfe.ppm_project.mapper;

import org.pfe.ppm_project.dto.TaskAssignmentDTO;
import org.pfe.ppm_project.entities.TaskAssignment;

/**
 * Mapper utilitaire pour convertir l'entite TaskAssignment en TaskAssignmentDTO.
 * Extrait les identifiants de la tache et de l'utilisateur au lieu des objets complets.
 */
public final class TaskAssignmentMapper {
    private TaskAssignmentMapper() {}

    // Convertit une affectation en DTO avec les IDs et le nom complet de l'utilisateur
    public static TaskAssignmentDTO toDTO(TaskAssignment ta) {
        var user = ta.getUser();
        String userName = user != null
                ? user.getFirstName() + " " + user.getLastName()
                : null;
        return new TaskAssignmentDTO(
                ta.getId(),
                ta.getTask() != null ? ta.getTask().getId() : null,
                user != null ? user.getId() : null,
                userName,
                ta.getAssignedHours(),
                ta.isActive(),
                ta.getCreatedAt()
        );
    }
}
