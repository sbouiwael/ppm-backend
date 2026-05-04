package org.pfe.ppm_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entree principal de l'application PPM (Project Portfolio Management).
 * Cette classe lance le conteneur Spring Boot qui auto-configure tous les beans,
 * demarre le serveur embarque et active le scan des composants du package org.pfe.ppm_project.
 *
 * @EnableScheduling active le moteur de taches planifiees de Spring (requis pour
 * DeadlineNotificationScheduler et tout autre @Scheduled bean).
 */
@SpringBootApplication // Combine @Configuration + @EnableAutoConfiguration + @ComponentScan
@EnableScheduling       // Active le support des @Scheduled dans tous les beans Spring
public class PpmProjectApplication {

    // Methode main standard — lance le contexte Spring et le serveur Tomcat embarque
    public static void main(String[] args) {
        SpringApplication.run(PpmProjectApplication.class, args);
    }

}
