package org.pfe.ppm_project.services;

import org.pfe.ppm_project.dto.DashboardDTO;
import org.pfe.ppm_project.dto.PortfolioDashboardDTO;

/**
 * Contrat du service de tableau de bord.
 *
 * Agrège les données de tous les domaines (projets, tâches, utilisateurs,
 * portefeuilles, affectations) en un seul DTO pour alimenter le dashboard.
 *
 * Implémentation actuelle : DashboardService (agrégation en mémoire via JPA).
 * Évolution possible : requêtes SQL optimisées / cache Redis / vue matérialisée.
 */
public interface IDashboardService {

    /**
     * Construit et retourne l'intégralité des données du tableau de bord opérationnel.
     * Toujours en lecture seule — pas d'effet de bord.
     */
    DashboardDTO buildDashboard();

    /**
     * Construit et retourne le tableau de bord exécutif du portefeuille.
     * Cible : PMO / ADMIN / PM — vision stratégique de la santé du portefeuille.
     * Inclut la classification de santé des projets (ON_TRACK / AT_RISK / DELAYED / COMPLETED).
     */
    PortfolioDashboardDTO buildPortfolioDashboard();
}