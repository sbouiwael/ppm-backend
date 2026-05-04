package org.pfe.ppm_project.enums;

/**
 * Enumeration des statuts possibles d'une tache.
 * Represente le cycle de vie d'une tache dans le systeme.
 */
public enum TaskStatus {
    NOT_STARTED, // Tache non commencee
    IN_PROGRESS, // Tache en cours d'execution
    DONE,        // Tache terminee
    BLOCKED      // Tache bloquee (en attente d'une dependance ou d'une decision)
}
