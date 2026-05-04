package org.pfe.ppm_project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifies the Spring application context loads without errors.
 * JWT_SECRET is provided by src/test/resources/application.properties.
 * Requires a running MySQL instance (provided by CI service container or local dev DB).
 */
@SpringBootTest
@ActiveProfiles("test")
class PpmProjectApplicationTests {

    @Test
    void contextLoads() {
        // Passes if the full Spring context assembles without exceptions.
        // This catches misconfigured beans, missing properties, and wiring errors.
    }

}
