package org.pfe.ppm_project.enums;

/**
 * Type d'action enregistree dans le journal d'audit PPM.
 * Couvre le cycle de vie complet des entites metier.
 *
 * Utilisé par AuditLogService pour categoriser chaque evenement
 * et permettre le filtrage dans l'ecran d'historique.
 */
public enum AuditAction {
    CREATE,         // Creation d'une entite (projet, tache, portefeuille, utilisateur)
    UPDATE,         // Modification d'une entite
    DELETE,         // Suppression logique (desactivation)
    STATUS_CHANGE,  // Changement de statut metier (ex: NOT_STARTED -> IN_PROGRESS -> DONE)
    ASSIGN,         // Affectation d'un utilisateur a une tache
    UNASSIGN,       // Retrait d'une affectation
    DEPENDENCY_ADD, // Ajout d'une dependance entre taches (Gantt)
    DEPENDENCY_REMOVE // Suppression d'une dependance entre taches
}