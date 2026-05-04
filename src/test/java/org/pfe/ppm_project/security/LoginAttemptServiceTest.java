package org.pfe.ppm_project.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour LoginAttemptService.
 * Verifie la logique de protection anti-force-brute (comptage, blocage, deblocage).
 */
class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    @Test
    @DisplayName("Un email inconnu ne doit pas etre bloque")
    void unknownEmail_isNotBlocked() {
        assertThat(service.isBlocked("unknown@biat.com.tn")).isFalse();
    }

    @Test
    @DisplayName("Apres MAX_ATTEMPTS-1 echecs, le compte n'est pas encore bloque")
    void belowMaxAttempts_isNotBlocked() {
        String email = "user@biat.com.tn";
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS - 1; i++) {
            service.recordFailure(email);
        }
        assertThat(service.isBlocked(email)).isFalse();
    }

    @Test
    @DisplayName("Apres MAX_ATTEMPTS echecs, le compte est bloque")
    void afterMaxAttempts_isBlocked() {
        String email = "user@biat.com.tn";
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure(email);
        }
        assertThat(service.isBlocked(email)).isTrue();
    }

    @Test
    @DisplayName("Apres MAX_ATTEMPTS+1 echecs, le compte reste bloque")
    void afterMoreThanMaxAttempts_remainsBlocked() {
        String email = "user@biat.com.tn";
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS + 3; i++) {
            service.recordFailure(email);
        }
        assertThat(service.isBlocked(email)).isTrue();
    }

    @Test
    @DisplayName("Apres une connexion reussie, le compte est debloque")
    void afterSuccess_isUnblocked() {
        String email = "user@biat.com.tn";
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure(email);
        }
        assertThat(service.isBlocked(email)).isTrue();

        service.recordSuccess(email);
        assertThat(service.isBlocked(email)).isFalse();
    }

    @Test
    @DisplayName("RetryAfterSeconds retourne 0 pour un email non bloque")
    void retryAfterSeconds_zero_whenNotBlocked() {
        assertThat(service.getRetryAfterSeconds("fresh@biat.com.tn")).isEqualTo(0L);
    }

    @Test
    @DisplayName("RetryAfterSeconds retourne une valeur positive apres blocage")
    void retryAfterSeconds_positive_whenBlocked() {
        String email = "locked@biat.com.tn";
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure(email);
        }
        assertThat(service.getRetryAfterSeconds(email)).isGreaterThan(0L);
    }

    @Test
    @DisplayName("L'email est insensible a la casse")
    void emailIsCaseInsensitive() {
        service.recordFailure("Admin@BIAT.com.tn");
        assertThat(service.isBlocked("admin@biat.com.tn")).isFalse(); // Only 1 failure, not blocked yet

        // Accumulate to threshold
        for (int i = 1; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure("ADMIN@biat.COM.TN");
        }
        assertThat(service.isBlocked("admin@biat.com.tn")).isTrue();
        assertThat(service.isBlocked("ADMIN@BIAT.COM.TN")).isTrue();
    }

    @Test
    @DisplayName("null email ne provoque pas de NullPointerException")
    void nullEmail_doesNotThrow() {
        assertThat(service.isBlocked(null)).isFalse();
        service.recordFailure(null); // should not throw
        service.recordSuccess(null); // should not throw
        assertThat(service.getRetryAfterSeconds(null)).isEqualTo(0L);
    }
}