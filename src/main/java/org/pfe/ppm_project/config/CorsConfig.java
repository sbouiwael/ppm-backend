package org.pfe.ppm_project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration CORS (Cross-Origin Resource Sharing).
 * Les origines autorisees sont configurees via la variable d'environnement CORS_ALLOWED_ORIGINS.
 * Par defaut : http://localhost:4200 pour le developpement local.
 * En production : URL du frontend Angular deploye (ex: https://ppm.biat.com.tn).
 */
@Configuration
public class CorsConfig {

    /**
     * Origines CORS autorisees — configurables via variable d'environnement.
     * Plusieurs origines possibles separees par des virgules.
     * Ex: CORS_ALLOWED_ORIGINS=https://ppm.biat.com.tn,https://ppm-staging.biat.com.tn
     */
    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;

    // Definit les regles CORS pour toutes les routes /api/**
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins) // Origines configurables via env var CORS_ALLOWED_ORIGINS
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*") // Accepte tous les headers (dont Authorization pour le JWT)
                        .exposedHeaders("Authorization")
                        .allowCredentials(true)
                        .maxAge(3600); // Cache preflight 1h pour reduire les requetes OPTIONS
            }
        };
    }
}
