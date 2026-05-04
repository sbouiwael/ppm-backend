package org.pfe.ppm_project.controller;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.LoginRequest;
import org.pfe.ppm_project.dto.LoginResponse;
import org.pfe.ppm_project.entities.User;
import org.pfe.ppm_project.repositories.UserRepository;
import org.pfe.ppm_project.security.JwtUtil;
import org.pfe.ppm_project.security.ILoginAttemptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller d'authentification — gere le login et la verification de l'identite.
 * Expose des endpoints publics (pas besoin de token) sous /api/auth/.
 * Le login retourne un token JWT que le frontend stocke et envoie dans chaque requete.
 * L'endpoint /me permet au frontend de verifier si le token est encore valide au rechargement de page.
 */
@RestController
@RequestMapping("/api/auth") // Endpoint public — autorise dans SecurityConfig sans authentification
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // BCrypt pour verifier le mot de passe
    private final JwtUtil jwtUtil;
    private final ILoginAttemptService loginAttemptService; // Protection anti-force-brute

    // Authentification par email/mot de passe — retourne un token JWT si les credentials sont valides
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email();

        // Protection anti-force-brute : bloquer apres MAX_ATTEMPTS echecs en 15 minutes
        if (loginAttemptService.isBlocked(email)) {
            long retryAfter = loginAttemptService.getRetryAfterSeconds(email);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter))
                    .body(Map.of("error", "Too many failed attempts. Try again in " + retryAfter + " seconds."));
        }

        // Recherche l'utilisateur par email — retourne null si non trouve
        User user = userRepository.findByEmail(email).orElse(null);

        // Verifie que l'utilisateur existe ET que son compte est actif
        if (user == null || !user.isActive()) {
            loginAttemptService.recordFailure(email);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or account inactive"));
        }

        // Compare le mot de passe en clair avec le hash BCrypt stocke en base
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.recordFailure(email);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid password"));
        }

        // Connexion reussie : effacer le compteur d'echecs
        loginAttemptService.recordSuccess(email);

        // Genere un token JWT contenant l'identite de l'utilisateur
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        // Retourne le token + les infos utilisateur pour le frontend
        return ResponseEntity.ok(new LoginResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        ));
    }

    // Verification du token — le frontend appelle cet endpoint au rechargement pour restaurer la session
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String header) {
        // Verifie la presence et le format du header Authorization
        if (header == null || !header.startsWith("Bearer ") || header.length() < 8) {
            return ResponseEntity.status(401).body(Map.of("error", "No token"));
        }

        String token = header.substring(7);
        // Valide le token (signature + expiration)
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        // Extrait l'ID utilisateur du token et retourne ses infos si le compte est toujours actif
        Long userId = jwtUtil.getUserIdFromToken(token);
        return userRepository.findById(userId)
                .filter(User::isActive) // Verifie que le compte n'a pas ete desactive entre-temps
                .map(user -> ResponseEntity.ok(new LoginResponse(
                        token,
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getRole()
                )))
                .orElse(ResponseEntity.status(401).build());
    }
}
