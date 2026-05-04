package org.pfe.ppm_project.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilitaire JWT (JSON Web Token) — responsable de la generation, validation et extraction des tokens.
 * Le token contient : email (subject), userId, role, date d'emission et date d'expiration.
 * La cle secrete et la duree d'expiration sont configurees dans application.properties (jwt.secret, jwt.expiration-ms).
 */
@Component // Bean singleton — injecte dans JwtAuthenticationFilter et AuthController
public class JwtUtil {

    // Cle secrete HMAC-SHA pour signer et verifier les tokens
    private final SecretKey key;
    // Duree de validite du token en millisecondes (configuree dans application.properties)
    private final long expirationMs;

    // Injection des valeurs depuis application.properties via @Value
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // Genere un token JWT contenant l'identite de l'utilisateur (email, id, role)
    public String generateToken(Long userId, String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email) // Le subject est l'email — identifiant principal
                .claim("userId", userId) // Claim personnalise pour l'ID utilisateur
                .claim("role", role) // Claim personnalise pour le role (ADMIN, PM, DEV, etc.)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs)) // Expiration calculee a partir de maintenant
                .signWith(key) // Signature HMAC avec la cle secrete
                .compact();
    }

    // Extrait l'email (subject) du token — utilise pour identifier l'utilisateur
    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    // Extrait le role du token — utilise pour les autorisations (@PreAuthorize)
    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // Extrait l'ID utilisateur du token — utilise pour acceder aux donnees de l'utilisateur
    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    // Valide le token (signature + expiration) — retourne false si invalide ou expire
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token invalide, expire, mal forme, ou signature incorrecte
            return false;
        }
    }

    // Parse et verifie le token — leve une exception si la signature ou l'expiration est invalide
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key) // Verifie la signature avec la cle secrete
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
