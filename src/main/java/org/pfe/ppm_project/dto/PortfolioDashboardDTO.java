package org.pfe.ppm_project.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO du tableau de bord executif de portefeuille PPM.
 *
 * CIBLE : PMO, ADMIN, PM — vision strategique et operationnelle du portefeuille.
 *
 * METRIQUES CLES (KPI Microsoft PPM) :
 *   - activeProjectsCount     : projets en cours (active=true, progress < 100)
 *   - delayedProjectsCount    : projets dont endDate < aujourd'hui ET progress < 100
 *   - completedProjectsCount  : projets avec progress = 100
 *   - averageProgress         : progression moyenne ponderee de tous les projets actifs
 *   - overdueTasks            : taches dont endDate < aujourd'hui et non terminees
 *
 * STRUCTURE DE SANTE DES PROJETS :
 *   Chaque ProjectHealthSummary indique le statut de sante :
 *     ON_TRACK  — endDate >= aujourd'hui OU progress = 100
 *     AT_RISK   — endDate dans moins de 7 jours ET progress < 80
 *     DELAYED   — endDate < aujourd'hui ET progress < 100
 *     COMPLETED — progress = 100
 */
public record PortfolioDashboardDTO(

        // ── KPI globaux ────────────────────────────────────────────────────
        int totalPortfolios,
        int totalActiveProjects,
        int delayedProjectsCount,
        int completedProjectsCount,
        int onTrackProjectsCount,
        int atRiskProjectsCount,
        double averageProgress,

        // ── Taches ────────────────────────────────────────────────────────
        int totalActiveTasks,
        int overdueTasks,
        Map<String, Integer> tasksByStatus,

        // ── Ressources ────────────────────────────────────────────────────
        int totalActiveUsers,
        Map<String, Integer> usersByRole,

        // ── Vision projet par projet ───────────────────────────────────────
        List<ProjectHealthSummary> projectHealthOverview,

        // ── Vision portefeuille par portefeuille ───────────────────────────
        List<PortfolioSummary> portfolioSummaries

) {

    /**
     * Resume de sante d'un projet individuel.
     * Utilise pour le tableau "Project Health Overview" dans le dashboard executif.
     *
     * healthStatus : ON_TRACK | AT_RISK | DELAYED | COMPLETED
     * Calcule par DashboardService selon la date du jour et la progression.
     */
    public record ProjectHealthSummary(
            Long id,
            String name,
            int progress,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate baselineEndDate,
            boolean isActive,
            String healthStatus,
            Long managerId,
            Long portfolioId,
            String portfolioName,
            int taskCount
    ) {}

    /**
     * Resume d'un portefeuille : KPIs agreges de tous ses projets.
     * Utile pour la vue "Portfolio Overview Cards" dans le dashboard executif.
     */
    public record PortfolioSummary(
            Long id,
            String name,
            int projectCount,
            int activeProjectCount,
            int delayedProjectCount,
            int completedProjectCount,
            int avgProgress
    ) {}
}
