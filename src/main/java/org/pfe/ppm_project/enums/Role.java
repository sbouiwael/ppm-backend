package org.pfe.ppm_project.enums;

/**
 * Enumeration des roles utilisateur dans le systeme de gestion de projets.
 * Chaque utilisateur possede un seul role qui determine ses permissions.
 */
public enum Role {
    PM,     // Chef de projet (Project Manager)
    PMO,    // Bureau de gestion de projets (Project Management Office)
    DEV,    // Developpeur
    QA,     // Testeur / Assurance qualite
    DEVOPS, // Ingenieur DevOps
    RH,     // Ressources humaines
    ADMIN   // Administrateur systeme
}
