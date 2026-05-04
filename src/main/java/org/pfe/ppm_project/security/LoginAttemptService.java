package org.pfe.ppm_project.security;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de protection contre les attaques par force brute sur l'endpoint /api/auth/login.
 * Bloque un compte pendant BLOCK_DURATION_MS apres MAX_ATTEMPTS echecs consecutifs.
 * Utilise un cache en memoire (ConcurrentHashMap) : reset au redemarrage du serveur.
 *
 * Note cloud : pour un deploiement multi-pod, remplacer par un cache Redis partage.
 */
@Service
public class LoginAttemptService implements ILoginAttemptService {

    /** Nombre maximal de tentatives echouees avant blocage */
    static final int MAX_ATTEMPTS = 5;

    /** Duree du blocage en millisecondes (15 minutes) */
    static final long BLOCK_DURATION_MS = 15L * 60 * 1000;

    /** Cache des tentatives : email -> [nombreEchecs, timestampPremierEchec] */
    private final ConcurrentHashMap<String, long[]> attemptCache = new ConcurrentHashMap<>();

    /**
     * Verifie si l'email est actuellement bloque (trop de tentatives recentes).
     *
     * @param email adresse email testee
     * @return true si l'acces est bloque, false sinon
     */
    public boolean isBlocked(String email) {
        if (email == null) return false;
        long[] data = attemptCache.get(email.toLowerCase());
        if (data == null) return false;

        // Si la fenetre de blocage est expiree, nettoyer et autoriser
        if (System.currentTimeMillis() - data[1] > BLOCK_DURATION_MS) {
            attemptCache.remove(email.toLowerCase());
            return false;
        }

        return data[0] >= MAX_ATTEMPTS;
    }

    /**
     * Enregistre un echec de connexion pour cet email.
     *
     * @param email adresse email concernee
     */
    public void recordFailure(String email) {
        if (email == null) return;
        String key = email.toLowerCase();
        attemptCache.compute(key, (k, v) -> {
            long now = System.currentTimeMillis();
            if (v == null || now - v[1] > BLOCK_DURATION_MS) {
                // Premiere tentative ou fenetre expiree : reinitialiser
                return new long[]{1L, now};
            }
            // Incrementer le compteur dans la fenetre existante
            return new long[]{v[0] + 1, v[1]};
        });
    }

    /**
     * Efface les tentatives apres une connexion reussie (reset du compteur).
     *
     * @param email adresse email concernee
     */
    public void recordSuccess(String email) {
        if (email == null) return;
        attemptCache.remove(email.toLowerCase());
    }

    /**
     * Retourne le nombre de secondes restantes avant deblocage.
     * Utilise pour le header Retry-After dans la reponse HTTP 429.
     *
     * @param email adresse email concernee
     * @return secondes restantes, ou 0 si non bloque
     */
    public long getRetryAfterSeconds(String email) {
        if (email == null) return 0;
        long[] data = attemptCache.get(email.toLowerCase());
        if (data == null) return 0;
        long elapsed = System.currentTimeMillis() - data[1];
        long remaining = BLOCK_DURATION_MS - elapsed;
        return remaining > 0 ? (long) Math.ceil(remaining / 1000.0) : 0;
    }
}