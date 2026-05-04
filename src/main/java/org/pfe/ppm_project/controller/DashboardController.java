package org.pfe.ppm_project.controller;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.DashboardDTO;
import org.pfe.ppm_project.dto.PortfolioDashboardDTO;
import org.pfe.ppm_project.services.IDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller du tableau de bord — délègue entièrement à IDashboardService.
 *
 * ARCHITECTURE FIX (2026-04): Ce controller n'injecte plus de repositories.
 * Toute la logique d'agrégation est dans DashboardService, ce qui permet :
 * - des tests unitaires du service sans SpringBootTest
 * - le remplacement de l'implémentation (SQL agrégé, cache) sans toucher le controller
 * - le respect de la séparation des couches Controller / Service / Repository
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;

    /** Tableau de bord operationnel — accessible a tous les utilisateurs authentifies. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardDTO> getDashboard() {
        return ResponseEntity.ok(dashboardService.buildDashboard());
    }

    /**
     * Tableau de bord executif du portefeuille (Wave 2).
     * Cible PMO / ADMIN / PM — vision sante du portefeuille avec classification des projets.
     * Retourne : KPIs, classification ON_TRACK/AT_RISK/DELAYED/COMPLETED,
     *            taches en retard, charge ressources, statistiques par portefeuille.
     */
    @GetMapping("/portfolio")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')")
    public ResponseEntity<PortfolioDashboardDTO> getPortfolioDashboard() {
        return ResponseEntity.ok(dashboardService.buildPortfolioDashboard());
    }
}