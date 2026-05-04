package org.pfe.ppm_project.config;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.entities.User;
import org.pfe.ppm_project.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Migration unique des mots de passe en clair vers BCrypt.
 * S'execute au demarrage de l'application apres le CalendarDataSeeder.
 * Detecte les mots de passe non haches en verifiant l'absence du prefixe BCrypt ($2a$ ou $2b$).
 * Necessaire car les premiers utilisateurs ont pu etre crees avec des mots de passe en clair.
 */
@Component
@Order(10) // Priorite d'execution — s'execute apres CalendarDataSeeder (ordre par defaut = 0)
@RequiredArgsConstructor
public class PasswordMigrationSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordMigrationSeeder.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // BCryptPasswordEncoder injecte depuis SecurityConfig

    @Override
    public void run(String... args) {
        List<User> users = userRepository.findAll();
        int migrated = 0;

        for (User user : users) {
            String pwd = user.getPassword();
            // Un mot de passe BCrypt commence toujours par "$2a$" ou "$2b$"
            // Si ce n'est pas le cas, c'est un mot de passe en clair a migrer
            if (pwd != null && !pwd.startsWith("$2a$") && !pwd.startsWith("$2b$")) {
                user.setPassword(passwordEncoder.encode(pwd));
                userRepository.save(user);
                migrated++;
            }
        }

        // Log uniquement si des mots de passe ont ete migres — evite le bruit dans les logs
        if (migrated > 0) {
            log.info("Migrated {} user password(s) from plaintext to BCrypt", migrated);
        }
    }
}
