package org.pfe.ppm_project.config;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Configuration centrale de la securite Spring Security.
 *
 * ARCHITECTURE — mode stateless JWT :
 *   Chaque requete HTTP est autonome et doit contenir un token Bearer dans
 *   le header Authorization. Il n'y a pas de session cote serveur (STATELESS).
 *   Cela permet de scaler horizontalement (plusieurs pods, aucun partage d'etat).
 *
 * ORDRE DE LA CHAINE DE FILTRES (simplifie) :
 *   1. CorsFilter (CORS avant auth, pour les requetes preflight OPTIONS)
 *   2. JwtAuthenticationFilter  ← insere ici avec addFilterBefore()
 *      → extrait le token, valide la signature, injecte l'identite dans SecurityContext
 *   3. UsernamePasswordAuthenticationFilter (desactive en pratique — on n'utilise pas formLogin)
 *   4. AuthorizationFilter → verifie les regles authorizeHttpRequests()
 *   5. @PreAuthorize → verifie les annotations sur les methodes des controllers
 *
 * DOUBLE ENFORCEMENT RBAC :
 *   - Couche 1 (ici) : authorizeHttpRequests() bloque les requetes sans token valide
 *   - Couche 2 : @PreAuthorize("hasRole('ADMIN')") sur les methodes → verification du role
 *   Si quelqu'un bypass le filtre, la couche 2 refusera quand meme l'acces.
 *
 * FICHIER SUIVANT A LIRE : security/JwtAuthenticationFilter.java
 */
@Configuration
@EnableMethodSecurity // Active les annotations @PreAuthorize et @PostAuthorize sur les methodes
@RequiredArgsConstructor // Injection par constructeur via Lombok — evite le @Autowired
public class SecurityConfig {

    // Filtre JWT injecte — intercepte chaque requete pour verifier le token
    // Lire : security/JwtAuthenticationFilter.java
    private final JwtAuthenticationFilter jwtFilter;

    // Bean d'encodage des mots de passe — BCrypt est un algorithme de hachage securise avec salt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Chaine de filtres de securite — definit les regles d'acces aux endpoints
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF desactive : les attaques CSRF exploitent les cookies de session.
            // Comme on n'utilise pas de session (STATELESS + JWT), CSRF n'est pas applicable.
            .csrf(csrf -> csrf.disable())
            // CORS active — la configuration detaillee est dans CorsConfig.java
            .cors(cors -> cors.configure(http))
            // STATELESS : Spring ne cree jamais de session HTTP. Chaque requete doit
            // s'authentifier seule via son token Bearer. Essentiel pour le scaling horizontal.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Headers de securite HTTP (defense en profondeur)
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()) // Protege contre clickjacking
                .contentTypeOptions(ct -> {}) // X-Content-Type-Options: nosniff
                .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000) // HSTS 1 an — force HTTPS en production
                )
            )
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics — accessibles sans token (login + documentation Swagger)
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                // Actuator health probes pour Kubernetes (liveness + readiness) — sans token
                .requestMatchers("/actuator/health/**", "/actuator/health").permitAll()

                // Endpoint de scraping Prometheus — autorise SANS token.
                // Justification : Prometheus (Operator) scrape ce endpoint via un ServiceMonitor
                // sur le Service ClusterIP interne (jamais expose hors du cluster, ni via l'ingress).
                // On ouvre UNIQUEMENT /actuator/prometheus — le reste d'actuator reste ADMIN.
                // En production durcie : isoler l'actuator sur un management.server.port dedie
                // non route par l'ingress, ou ajouter une NetworkPolicy limitant la source au
                // namespace monitoring.
                .requestMatchers("/actuator/prometheus").permitAll()

                // Actuator metrics et autres endpoints — proteges (ADMIN uniquement)
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // Tous les autres endpoints /api/** necessitent un token JWT valide
                .requestMatchers("/api/**").authenticated()

                // Tout le reste (fichiers statiques, etc.) est accessible librement
                .anyRequest().permitAll()
            )
            // Insere le filtre JWT AVANT le filtre d'authentification par defaut de Spring
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
