package org.pfe.ppm_project.security;

/**
 * Contrat du service de protection anti-force-brute.
 *
 * Implémentation actuelle : LoginAttemptService (ConcurrentHashMap en mémoire).
 * Limite : état non partagé entre pods Kubernetes — brute-force possible en
 *          ciblant alternativement plusieurs réplicas.
 *
 * Implémentation future (multi-pod) : RedisLoginAttemptService
 *   → dépend du fournisseur cloud (ElastiCache / Azure Cache / Memorystore)
 *   → à implémenter une fois le provider choisi.
 *
 * Pour activer l'implémentation Redis, déclarer la classe concrète avec
 * @Primary ou utiliser @ConditionalOnProperty(name="login.attempt.store", havingValue="redis").
 */
public interface ILoginAttemptService {

    /**
     * @return true si l'email est temporairement bloqué (trop de tentatives échouées)
     */
    boolean isBlocked(String email);

    /**
     * Enregistre un échec de connexion pour cet email.
     * Incrémente le compteur ; bloque si le seuil MAX_ATTEMPTS est atteint.
     */
    void recordFailure(String email);

    /**
     * Réinitialise le compteur après une connexion réussie.
     */
    void recordSuccess(String email);

    /**
     * @return nombre de secondes restantes avant déblocage, ou 0 si non bloqué
     */
    long getRetryAfterSeconds(String email);
}