package org.pfe.ppm_project.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entite JPA representant l'affectation d'un utilisateur a une tache.
 * Chaque affectation definit le nombre d'heures assignees a une ressource
 * sur une tache donnee. La contrainte d'unicite empeche les doublons (tache + utilisateur).
 */
@Entity
@Table(
        name = "task_assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "user_id"}) // Un utilisateur ne peut etre affecte qu'une seule fois a une meme tache
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssignment {

    // Identifiant unique auto-genere
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tache concernee par l'affectation (obligatoire)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    // Utilisateur (ressource) affecte a la tache (obligatoire)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Nombre d'heures assignees a cette ressource sur cette tache
    @Column(nullable = false)
    private Integer assignedHours;

    // Indicateur de suppression logique
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // Date de creation de l'affectation (non modifiable)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Callback JPA : initialise la date et normalise avant la premiere sauvegarde
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        normalize();
    }

    // Callback JPA : normalise avant chaque mise a jour
    @PreUpdate
    protected void onUpdate() {
        normalize();
    }

    // Les heures assignees ne peuvent pas etre negatives
    private void normalize() {
        if (this.assignedHours == null || this.assignedHours < 0) {
            this.assignedHours = 0;
        }
    }
}
