package org.pfe.ppm_project.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilitaire statique pour extraire l'identite de l'acteur courant
 * depuis le SecurityContextHolder de Spring Security.
 *
 * Utilise par AuditLogService pour savoir qui effectue une action
 * sans avoir besoin d'injecter le contexte HTTP dans les services metier.
 *
 * Retourne des valeurs par defaut si aucun utilisateur n'est authentifie
 * (cas des tests unitaires ou des executions programmatiques sans JWT).
 */
public final class AuditContextUtil {

    private AuditContextUtil() {}

    /**
     * Retourne l'ID de l'utilisateur courant depuis les details du token JWT.
     * JwtAuthenticationFilter stocke l'userId via authToken.setDetails(userId).
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(auth)) return null;
        Object details = auth.getDetails();
        return (details instanceof Long) ? (Long) details : null;
    }

    /**
     * Retourne l'email de l'utilisateur courant (principal du token JWT).
     */
    public static String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(auth)) return "system";
        String name = auth.getName();
        return (name != null && !name.isBlank()) ? name : "system";
    }

    /**
     * Retourne le role de l'utilisateur courant sans le prefixe ROLE_.
     */
    public static String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(auth)) return "SYSTEM";
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(r -> r.replace("ROLE_", ""))
                .orElse("UNKNOWN");
    }

    private static boolean isAuthenticated(Authentication auth) {
        return auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }
}