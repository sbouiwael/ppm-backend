package org.pfe.ppm_project.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.pfe.ppm_project.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de lecture/ecriture pour une tache.
 * Transfere toutes les informations d'une tache vers le client,
 * y compris les champs Gantt (WBS, duree, baseline) et le suivi d'avancement.
 */
public record TaskDTO(
        Long id,                        // Identifiant de la tache

        @NotBlank(message = "Task name is required")
        @Size(min = 1, max = 255, message = "Task name must be between 1 and 255 characters")
        String name,                    // Nom de la tache

        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,             // Description detaillee

        @NotNull(message = "Project ID is required")
        @Positive(message = "Project ID must be positive")
        Long projectId,                 // ID du projet parent

        Long parentTaskId,              // ID de la tache parente (null = tache racine)

        // Champs de planification Gantt
        String wbsNumber,               // Numero WBS (Work Breakdown Structure)
        String mode,                    // Mode (TASK, MILESTONE, etc.)

        @Positive(message = "Duration must be positive")
        Double durationDays,            // Duree estimee en jours ouvres

        @Min(value = 0, message = "Work hours cannot be negative")
        Double workHours,               // Charge de travail en heures

        // Donnees de reference (baseline)
        Double baselineDurationDays,    // Duree de reference
        LocalDate baselineStartDate,    // Date de debut de reference
        LocalDate baselineEndDate,      // Date de fin de reference

        @Min(value = 0, message = "Actual work hours cannot be negative")
        Double actualWorkHours,         // Heures reellement travaillees
        String calendarName,            // Nom du calendrier associe

        Integer sortOrder,              // Ordre d'affichage

        LocalDate startDate,            // Date de debut effective
        LocalDate endDate,              // Date de fin effective

        TaskStatus status,              // Statut (NOT_STARTED, IN_PROGRESS, DONE, BLOCKED)

        @Min(value = 0, message = "Progress must be between 0 and 100")
        @Max(value = 100, message = "Progress must be between 0 and 100")
        Integer progress,               // Pourcentage d'avancement (0-100)
        boolean active,                 // Tache active ou desactivee
        LocalDateTime createdAt,        // Date de creation

        // Champs de lisibilite — remplis uniquement en lecture (null en creation/modification)
        String projectName,             // Nom du projet (pour affichage, non obligatoire en ecriture)
        String parentTaskName,          // Nom de la tache parente (pour affichage, null si tache racine)

        // IDs des utilisateurs ACTIFS affectes a cette tache (null si non charge, ex: en liste)
        // Rempli uniquement par GET /api/tasks/{id} — pas en liste (evite le N+1)
        List<Long> assignedUserIds,     // IDs des ressources affectees (actives uniquement)

        // Indique si cette tache a des sous-taches actives (calculé, non stocké)
        // Utilise pour verrouiller l'edition directe de progress/status dans l'UI
        boolean hasChildren
) {}
