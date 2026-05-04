package org.pfe.ppm_project.enums;

/**
 * Type d'evenement generant une notification dans le systeme PPM.
 * Determine le canal (qui recoit) et l'urgence (visuel) de la notification.
 */
public enum NotificationType {
    TASK_ASSIGNED,        // Une tache m'a ete assignee
    DEADLINE_APPROACHING, // L'echeance d'une tache approche (< 3 jours)
    TASK_OVERDUE,         // Une tache est en retard (endDate depassee, non terminee)
    OVERLOAD_WARNING,     // Le projet depasse la capacite de ressources prevue
    PROJECT_UPDATE,       // Un projet a ete mis a jour par le gestionnaire
    DEPENDENCY_BLOCKED    // Une dependance bloque la progression d'une tache
}