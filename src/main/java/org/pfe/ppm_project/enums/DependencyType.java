package org.pfe.ppm_project.enums;

/**
 * Enumeration des types de dependances entre taches (diagramme de Gantt).
 * Definit la relation temporelle entre un predecesseur et un successeur.
 */
public enum DependencyType {
    FS, // Fin-Debut (Finish to Start) : le successeur commence quand le predecesseur se termine
    SS, // Debut-Debut (Start to Start) : les deux taches commencent en meme temps
    FF, // Fin-Fin (Finish to Finish) : les deux taches se terminent en meme temps
    SF  // Debut-Fin (Start to Finish) : le successeur se termine quand le predecesseur commence
}
