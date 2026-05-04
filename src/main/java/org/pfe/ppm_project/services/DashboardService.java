package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.DashboardDTO;
import org.pfe.ppm_project.dto.PortfolioDashboardDTO;
import org.pfe.ppm_project.entities.*;
import org.pfe.ppm_project.enums.TaskStatus;
import org.pfe.ppm_project.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implémentation du service de tableau de bord.
 *
 * ARCHITECTURE FIX (2026-04): Extrait de DashboardController où les repositories
 * étaient injectés directement dans le controller, violant la séparation des couches.
 *
 * Stratégie d'agrégation :
 * - Toutes les entités sont chargées en une passe (findAll) pour éviter le N+1.
 * - Un index tasksByProject est construit en mémoire pour les statistiques par projet.
 * - La charge de travail (assignmentRepo.findByUserId) reste O(N) par utilisateur actif.
 *   À optimiser avec une requête agrégée si le volume dépasse ~500 utilisateurs.
 *
 * Toutes les méthodes sont en lecture seule (@Transactional readOnly = true),
 * ce qui permet à Hibernate de désactiver le dirty-check et d'optimiser le flush.
 */
@Service
@RequiredArgsConstructor
public class DashboardService implements IDashboardService {

    private final ProjectRepository projectRepo;
    private final TaskRepository taskRepo;
    private final UserRepository userRepo;
    private final PortefeuilleRepository portefeuilleRepo;
    private final TaskAssignmentRepository assignmentRepo;

    @Override
    @Transactional(readOnly = true)
    public DashboardDTO buildDashboard() {

        List<Project>      projects   = projectRepo.findAll();
        List<User>         users      = userRepo.findAll();
        List<Portefeuille> portfolios = portefeuilleRepo.findAll();
        List<Task>         allTasks   = taskRepo.findAll();

        // ── Index tâches par projet (évite N+1 dans la boucle projectStats) ──
        Map<Long, List<Task>> tasksByProject = allTasks.stream()
                .filter(t -> t.getProject() != null)
                .collect(Collectors.groupingBy(t -> t.getProject().getId()));

        // ── KPI globaux ──────────────────────────────────────────────────────
        int totalProjects  = projects.size();
        int activeProjects = (int) projects.stream().filter(Project::isActive).count();
        int totalUsers     = users.size();
        int activeUsers    = (int) users.stream().filter(User::isActive).count();

        // ── Répartition des tâches ACTIVES par statut ────────────────────────
        Map<String, Integer> tasksByStatus = new LinkedHashMap<>();
        tasksByStatus.put("NOT_STARTED", 0);
        tasksByStatus.put("IN_PROGRESS", 0);
        tasksByStatus.put("DONE",        0);
        tasksByStatus.put("BLOCKED",     0);
        for (Task t : allTasks) {
            if (t.isActive() && t.getStatus() != null) {
                tasksByStatus.merge(t.getStatus().name(), 1, Integer::sum);
            }
        }

        // ── Répartition des utilisateurs par rôle ────────────────────────────
        Map<String, Integer> usersByRole = new LinkedHashMap<>();
        for (User u : users) {
            if (u.getRole() != null) {
                usersByRole.merge(u.getRole().name(), 1, Integer::sum);
            }
        }

        // ── Statistiques par projet ──────────────────────────────────────────
        List<Map<String, Object>> projectStats = new ArrayList<>();
        for (Project p : projects) {
            List<Task> pTasks = tasksByProject.getOrDefault(p.getId(), List.of());
            Map<String, Object> ps = new LinkedHashMap<>();
            ps.put("id",              p.getId());
            ps.put("name",            p.getName());
            ps.put("progress",        p.getProgress());
            ps.put("startDate",       p.getStartDate());
            ps.put("endDate",         p.getEndDate());
            ps.put("baselineEndDate", p.getBaselineEndDate());
            ps.put("active",          p.isActive());
            ps.put("taskCount",       pTasks.size());
            ps.put("managerId",       p.getProjectManager() != null ? p.getProjectManager().getId() : null);
            projectStats.add(ps);
        }

        // ── Statistiques par portefeuille ────────────────────────────────────
        List<Map<String, Object>> portfolioStats = new ArrayList<>();
        for (Portefeuille pf : portfolios) {
            List<Project> pfProjects = pf.getProjects() != null ? pf.getProjects() : List.of();
            double avgProg = pfProjects.stream()
                    .filter(p -> p.getProgress() != null)
                    .mapToInt(Project::getProgress)
                    .average().orElse(0);
            Map<String, Object> pfs = new LinkedHashMap<>();
            pfs.put("id",           pf.getId());
            pfs.put("name",         pf.getNom());
            pfs.put("projectCount", pfProjects.size());
            pfs.put("avgProgress",  Math.round(avgProg));
            portfolioStats.add(pfs);
        }

        // ── Charge de travail par utilisateur actif ───────────────────────────
        // Une seule requête SQL agrégée : SUM(assignedHours) GROUP BY userId
        // Evite N+1 (précédemment 1 requête par utilisateur actif).
        Map<Long, Integer> hoursByUser = new HashMap<>();
        for (Object[] row : assignmentRepo.sumActiveAssignedHoursByUser()) {
            Long uid   = ((Number) row[0]).longValue();
            Integer hrs = ((Number) row[1]).intValue();
            hoursByUser.put(uid, hrs);
        }

        List<Map<String, Object>> workload = new ArrayList<>();
        for (User u : users) {
            if (!u.isActive()) continue;
            int totalHours = hoursByUser.getOrDefault(u.getId(), 0);
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("userId",             u.getId());
            w.put("firstName",          u.getFirstName());
            w.put("lastName",           u.getLastName());
            w.put("role",               u.getRole().name());
            w.put("weeklyCapacity",     u.getWeeklyCapacity());
            w.put("totalAssignedHours", totalHours);
            workload.add(w);
        }

        return new DashboardDTO(
                portfolios.size(), totalProjects, activeProjects,
                totalProjects - activeProjects, allTasks.size(),
                totalUsers, activeUsers,
                tasksByStatus, usersByRole,
                projectStats, portfolioStats, workload
        );
    }

    /**
     * Tableau de bord executif du portefeuille (Wave 2).
     *
     * CLASSIFICATION DE SANTE DES PROJETS (logique Microsoft PPM) :
     *   COMPLETED — progress = 100 (peu importe la date)
     *   DELAYED   — endDate < today ET progress < 100
     *   AT_RISK   — endDate dans <= 7 jours ET progress < 80 (risque d'etre en retard)
     *   ON_TRACK  — tous les autres projets actifs
     *
     * TACHES EN RETARD :
     *   endDate < today ET statut != DONE (taches actives uniquement)
     *
     * STRATEGIE DE CHARGEMENT :
     *   Meme approche que buildDashboard() : tout charge en une passe,
     *   index en memoire pour eviter N+1.
     */
    @Override
    @Transactional(readOnly = true)
    public PortfolioDashboardDTO buildPortfolioDashboard() {

        LocalDate today = LocalDate.now();

        List<Project>      projects   = projectRepo.findAll();
        List<User>         users      = userRepo.findAll();
        List<Portefeuille> portfolios = portefeuilleRepo.findAll();
        List<Task>         allTasks   = taskRepo.findAll();

        // ── Index taches actives par projet ─────────────────────────────────
        Map<Long, List<Task>> activTasksByProject = allTasks.stream()
                .filter(Task::isActive)
                .filter(t -> t.getProject() != null)
                .collect(Collectors.groupingBy(t -> t.getProject().getId()));

        // ── Index portefeuille par projet ────────────────────────────────────
        Map<Long, Portefeuille> portfolioByProjectId = new HashMap<>();
        for (Portefeuille pf : portfolios) {
            if (pf.getProjects() != null) {
                for (Project p : pf.getProjects()) {
                    portfolioByProjectId.put(p.getId(), pf);
                }
            }
        }

        // ── Classification de sante de chaque projet actif ──────────────────
        List<PortfolioDashboardDTO.ProjectHealthSummary> healthList = new ArrayList<>();
        int delayed = 0, completed = 0, atRisk = 0, onTrack = 0;

        for (Project p : projects) {
            if (!p.isActive()) continue;

            String health;
            if (p.getProgress() != null && p.getProgress() >= 100) {
                health = "COMPLETED";
                completed++;
            } else if (p.getEndDate() != null && p.getEndDate().isBefore(today)) {
                health = "DELAYED";
                delayed++;
            } else if (p.getEndDate() != null
                    && !p.getEndDate().isBefore(today)
                    && !p.getEndDate().isAfter(today.plusDays(7))
                    && (p.getProgress() == null || p.getProgress() < 80)) {
                // Echeance dans moins de 7 jours avec moins de 80% de progression
                health = "AT_RISK";
                atRisk++;
            } else {
                health = "ON_TRACK";
                onTrack++;
            }

            Portefeuille pf = portfolioByProjectId.get(p.getId());
            List<Task> pTasks = activTasksByProject.getOrDefault(p.getId(), List.of());

            healthList.add(new PortfolioDashboardDTO.ProjectHealthSummary(
                    p.getId(),
                    p.getName(),
                    p.getProgress() != null ? p.getProgress() : 0,
                    p.getStartDate(),
                    p.getEndDate(),
                    p.getBaselineEndDate(),
                    p.isActive(),
                    health,
                    p.getProjectManager() != null ? p.getProjectManager().getId() : null,
                    pf != null ? pf.getId() : null,
                    pf != null ? pf.getNom() : null,
                    pTasks.size()
            ));
        }

        // Tri : DELAYED en premier, puis AT_RISK, puis ON_TRACK, puis COMPLETED
        healthList.sort(Comparator.comparingInt(s -> switch (s.healthStatus()) {
            case "DELAYED"   -> 0;
            case "AT_RISK"   -> 1;
            case "ON_TRACK"  -> 2;
            case "COMPLETED" -> 3;
            default          -> 4;
        }));

        // ── Progression moyenne des projets actifs (non termines) ────────────
        // Conforme Microsoft PPM : base sur les projets actifs uniquement
        double avgProgress = projects.stream()
                .filter(Project::isActive)
                .mapToInt(p -> p.getProgress() != null ? p.getProgress() : 0)
                .average()
                .orElse(0.0);

        // ── Taches en retard (endDate < today, non terminees, actives) ────────
        long overdueTasks = allTasks.stream()
                .filter(Task::isActive)
                .filter(t -> t.getEndDate() != null && t.getEndDate().isBefore(today))
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .count();

        // ── Repartition des taches actives par statut ────────────────────────
        Map<String, Integer> tasksByStatus = new LinkedHashMap<>();
        tasksByStatus.put("NOT_STARTED", 0);
        tasksByStatus.put("IN_PROGRESS", 0);
        tasksByStatus.put("DONE",        0);
        tasksByStatus.put("BLOCKED",     0);
        allTasks.stream().filter(Task::isActive).forEach(t -> {
            if (t.getStatus() != null) {
                tasksByStatus.merge(t.getStatus().name(), 1, Integer::sum);
            }
        });

        // ── Repartition des utilisateurs actifs par role ─────────────────────
        Map<String, Integer> usersByRole = new LinkedHashMap<>();
        users.stream().filter(User::isActive).forEach(u -> {
            if (u.getRole() != null) {
                usersByRole.merge(u.getRole().name(), 1, Integer::sum);
            }
        });

        // ── Statistiques par portefeuille ─────────────────────────────────────
        List<PortfolioDashboardDTO.PortfolioSummary> portfolioSummaries = new ArrayList<>();
        for (Portefeuille pf : portfolios) {
            List<Project> pfProjects = pf.getProjects() != null ? pf.getProjects() : List.of();
            int pfActive    = (int) pfProjects.stream().filter(Project::isActive).count();
            int pfDelayed   = (int) pfProjects.stream()
                    .filter(Project::isActive)
                    .filter(p -> p.getEndDate() != null && p.getEndDate().isBefore(today)
                                 && (p.getProgress() == null || p.getProgress() < 100))
                    .count();
            int pfCompleted = (int) pfProjects.stream()
                    .filter(p -> p.getProgress() != null && p.getProgress() >= 100)
                    .count();
            double pfAvg = pfProjects.stream()
                    .filter(Project::isActive)
                    .mapToInt(p -> p.getProgress() != null ? p.getProgress() : 0)
                    .average().orElse(0);

            portfolioSummaries.add(new PortfolioDashboardDTO.PortfolioSummary(
                    pf.getId(), pf.getNom(),
                    pfProjects.size(), pfActive, pfDelayed, pfCompleted,
                    (int) Math.round(pfAvg)
            ));
        }

        long activeTasksTotal = allTasks.stream().filter(Task::isActive).count();
        long activeUsersCount = users.stream().filter(User::isActive).count();

        return new PortfolioDashboardDTO(
                portfolios.size(),
                (int) projects.stream().filter(Project::isActive).count(),
                delayed,
                completed,
                onTrack,
                atRisk,
                Math.round(avgProgress * 10.0) / 10.0,
                (int) activeTasksTotal,
                (int) overdueTasks,
                tasksByStatus,
                (int) activeUsersCount,
                usersByRole,
                healthList,
                portfolioSummaries
        );
    }
}