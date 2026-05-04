package org.pfe.ppm_project.entities;

import jakarta.persistence.*;
import lombok.*;
import org.pfe.ppm_project.enums.NotificationType;

import java.time.LocalDateTime;

/**
 * Entite JPA representant une notification destinee a un utilisateur PPM.
 *
 * DESIGN :
 *   - Chaque notification est proprietaire d'un utilisateur (recipientId).
 *   - L'etat de lecture (read) est par utilisateur.
 *   - Les notifications ne sont jamais modifiees apres creation, seul le flag
 *     'read' peut etre mis a jour (par l'utilisateur destinataire uniquement).
 *   - entityType + entityId permettent la navigation directe vers l'entite concernee.
 *
 * RBAC : une notification n'est visible que par son recipientId.
 */
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notif_recipient_read", columnList = "recipient_id, is_read"),
        @Index(name = "idx_notif_created",        columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID de l'utilisateur destinataire — cle de visibilite (RBAC). */
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    /** Type d'evenement — determine l'icone et la priorite dans l'UI. */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    /** Titre court de la notification (affiché en bold dans l'UI). */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** Message descriptif de la notification. */
    @Column(name = "message", length = 500)
    private String message;

    /**
     * Etat de lecture — false = non lu (compteur de badge dans l'en-tete),
     * true = lu (la notification reste visible mais ne compte pas dans le badge).
     * Colonne nommee is_read pour eviter la collision avec le mot-cle SQL READ.
     */
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    /** Type de l'entite concernee (PROJECT, TASK, etc.) — pour la navigation. */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /** ID de l'entite concernee — pour la navigation directe depuis la notification. */
    @Column(name = "entity_id")
    private Long entityId;

    /** Date de creation — notifications triees newest-first dans l'UI. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}