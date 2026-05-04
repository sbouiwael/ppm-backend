package org.pfe.ppm_project.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.pfe.ppm_project.enums.Role;

import java.time.LocalDateTime;

/**
 * Entite JPA representant un utilisateur du systeme.
 * Peut etre chef de projet (PM), membre d'equipe (DEV, QA, DEVOPS),
 * responsable RH, PMO ou administrateur selon son role.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    // Identifiant unique auto-genere
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Prenom de l'utilisateur (obligatoire)
    @Column(nullable = false, length = 100)
    private String firstName;

    // Nom de famille de l'utilisateur (obligatoire)
    @Column(nullable = false, length = 100)
    private String lastName;

    // Adresse email unique servant d'identifiant de connexion
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    // Mot de passe hache (BCrypt)
    @Column(nullable = false)
    private String password;

    // Role de l'utilisateur dans le systeme (PM, PMO, DEV, QA, DEVOPS, RH, ADMIN)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Capacite hebdomadaire en heures (pour le calcul de charge)
    @Column(nullable = false)
    private Integer weeklyCapacity;

    // Indicateur de suppression logique (false = desactive)
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // Date de creation du compte (non modifiable)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Callback JPA : initialise la date de creation et la capacite par defaut
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.weeklyCapacity == null || this.weeklyCapacity < 0) {
            this.weeklyCapacity = 0;
        }
    }
}
