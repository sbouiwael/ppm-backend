package org.pfe.ppm_project.dto;

/**
 * DTO retourne par GET /api/capacity.
 * Represente la capacite et la charge reelle d'un utilisateur actif.
 *
 * Logique Microsoft PPM — Resource Capacity Planning :
 *   - weeklyCapacity     : heures disponibles par semaine (donnee de profil utilisateur)
 *   - totalAssignedHours : total des heures affectees sur toutes les taches actives
 *   - utilizationPct     : totalAssignedHours / weeklyCapacity * 100
 *   - capacityStatus     : OVERLOADED (>100%), BALANCED (75-100%), UNDERUTILIZED (<75%)
 *
 * Note: totalAssignedHours represente une charge cumulee sur la duree des projets,
 *       pas une charge hebdomadaire. C'est une convention simplifiee pour la v1 —
 *       un modele temporel complet necessite la decomposition par semaine (Work Calendar).
 *
 * Utilise par le frontend "Capacity Planning" pour detecter les ressources surchargees
 * et les ressources sous-utilisees, et aider le PM/PMO a reequilibrer les affectations.
 */
public record ResourceCapacityDTO(

        /** Identifiant de l'utilisateur */
        Long userId,

        /** Prenom */
        String firstName,

        /** Nom de famille */
        String lastName,

        /** Email (pour identifier l'utilisateur dans les exports) */
        String email,

        /** Role dans le systeme (PM, PMO, DEV, QA, DEVOPS, RH, ADMIN) */
        String role,

        /** Capacite hebdomadaire en heures (profil utilisateur) */
        Integer weeklyCapacity,

        /** Total des heures affectees sur les taches actives */
        Integer totalAssignedHours,

        /**
         * Taux d'utilisation en pourcentage.
         * Formule : (totalAssignedHours / weeklyCapacity) * 100
         * Retourne 0.0 si weeklyCapacity = 0 pour eviter la division par zero.
         */
        double utilizationPct,

        /**
         * Statut de capacite :
         * - OVERLOADED     : utilizationPct > 100 (surcharge)
         * - BALANCED       : 75 <= utilizationPct <= 100 (equilibre)
         * - UNDERUTILIZED  : utilizationPct < 75 (sous-utilise)
         * - NO_CAPACITY    : weeklyCapacity = 0 (profil incomplet)
         */
        String capacityStatus
) {}