package org.pfe.ppm_project.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO regroupant toutes les donnees du tableau de bord.
 * Fournit les indicateurs cles (KPI), les statistiques par projet,
 * par portefeuille et la charge de travail des ressources.
 */
public record DashboardDTO(
        // === KPI globaux ===
        int totalPortfolios,        // Nombre total de portefeuilles
        int totalProjects,          // Nombre total de projets
        int totalActiveProjects,    // Nombre de projets actifs
        int totalInactiveProjects,  // Nombre de projets inactifs
        int totalTasks,             // Nombre total de taches
        int totalUsers,             // Nombre total d'utilisateurs
        int totalActiveUsers,       // Nombre d'utilisateurs actifs

        // Repartition des taches par statut (cle = statut, valeur = nombre)
        Map<String, Integer> tasksByStatus,

        // Repartition des utilisateurs par role (cle = role, valeur = nombre)
        Map<String, Integer> usersByRole,

        // Statistiques par projet : nom, progres, dates, nombre de taches, ID du chef de projet
        List<Map<String, Object>> projectStats,

        // Statistiques par portefeuille : nom, nombre de projets, avancement moyen
        List<Map<String, Object>> portfolioStats,

        // Charge de travail : userId, nom, role, capacite hebdomadaire, heures assignees
        List<Map<String, Object>> workload
) {}
