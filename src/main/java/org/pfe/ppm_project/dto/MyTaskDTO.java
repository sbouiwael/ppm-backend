package org.pfe.ppm_project.dto;

/**
 * DTO retourne par GET /api/assignments/me.
 * Agrege les informations d'une affectation avec les details de la tache et du projet.
 * Permet au frontend "My Tasks" d'afficher tout ce dont un DEV/QA/DEVOPS a besoin
 * sans appels supplementaires : statut, dates, avancement, projet parent.
 *
 * Logique Microsoft PPM : chaque contributeur technique voit uniquement
 * les taches qui lui sont affectees, avec leur contexte projet.
 */
public record MyTaskDTO(

        /** Identifiant de l'affectation (pour mise a jour des heures si besoin) */
        Long assignmentId,

        /** Identifiant de la tache */
        Long taskId,

        /** Nom de la tache */
        String taskName,

        /** Statut de la tache : NOT_STARTED, IN_PROGRESS, DONE, BLOCKED */
        String taskStatus,

        /** Avancement en pourcentage (0-100) */
        Integer taskProgress,

        /** Date de debut prevue (format ISO YYYY-MM-DD, peut etre null) */
        String startDate,

        /** Date de fin prevue (format ISO YYYY-MM-DD, peut etre null) */
        String endDate,

        /** Numero WBS (ex: "1.2.3") */
        String wbsNumber,

        /** Heures assignees a cet utilisateur pour cette tache */
        Integer assignedHours,

        /** Identifiant du projet auquel appartient la tache */
        Long projectId,

        /** Nom du projet (pour l'affichage et le filtre par projet) */
        String projectName,

        /** Duree prevue en jours (pour calcul surcharge) */
        Double durationDays,

        /** Heures de travail totales prevues sur la tache */
        Double workHours
) {}
