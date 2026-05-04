package org.pfe.ppm_project.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre d'authentification JWT — intercepete chaque requete HTTP entrante.
 * Etend OncePerRequestFilter pour garantir une seule execution par requete.
 * Extrait le token du header Authorization (ou du query param ?token= pour SSE),
 * le valide, puis injecte l'identite de l'utilisateur dans le SecurityContext.
 *
 * SSE NOTE : L'API EventSource du navigateur ne supporte pas les headers custom.
 * Pour les requetes SSE (/api/notifications/stream), le token JWT est accepte
 * via le query parameter ?token= en plus du header Bearer standard.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && jwtUtil.validateToken(token)) {
            String email    = jwtUtil.getEmailFromToken(token);
            String role     = jwtUtil.getRoleFromToken(token);
            Long   userId   = jwtUtil.getUserIdFromToken(token);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var authToken   = new UsernamePasswordAuthenticationToken(email, null, authorities);
            authToken.setDetails(userId);
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrait le token JWT depuis :
     * 1. Le header "Authorization: Bearer <token>" (requetes REST standard)
     * 2. Le query param "?token=<token>" (connexions SSE via EventSource)
     *
     * @return le token brut, ou null si absent ou invalide
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        // Fallback pour EventSource (SSE) qui ne supporte pas les headers custom
        String param = request.getParameter("token");
        if (param != null && !param.isBlank()) {
            return param;
        }
        return null;
    }
}
