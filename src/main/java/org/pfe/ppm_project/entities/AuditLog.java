package org.pfe.ppm_project.entities;

import jakarta.persistence.*;
import lombok.*;
import org.pfe.ppm_project.enums.AuditAction;

import java.time.LocalDateTime;

/**
 * Entite JPA representant une entree dans le journal d'audit PPM.
 *
 * DESIGN DECISION — Denormalization intentionnelle :
 *   Les champs actorEmail, actorRole, entityName, projectName sont copies au moment
 *   de l'action, pas references par cle etrangere. Cela garantit que l'historique
 *   reste lisible meme si un utilisateur est renomme ou un projet desactive.
 *   C'est la pratique standard pour les systemes d'audit (event sourcing).
 *
 * INDEX STRATEGY :
 *   idx_audit_entity   — recherche par entite (ex: voir l'historique du projet X)
 *   idx_audit_actor    — recherche par acteur (ex: voir toutes les actions de user Y)
 *   idx_audit_ts       — filtrage par plage de dates
 *   idx_audit_project  — filtrage par projet (vue PM)
 */
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_entity",  columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_actor",   columnList = "actor_id"),
        @Index(name = "idx_audit_ts",      columnList = "timestamp"),
        @Index(name = "idx_audit_project", columnList = "project_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Acteur ─────────────────────────────────────────────────────────────

    /** ID de l'utilisateur ayant effectue l'action (peut etre null pour les actions systeme). */
    @Column(name = "actor_id")
    private Long actorId;

    /** Email de l'acteur — copie denormalisee pour lisibilite de l'historique. */
    @Column(name = "actor_email", nullable = false, length = 200)
    private String actorEmail;

    /** Role de l'acteur au moment de l'action (ADMIN, PMO, PM, etc.). */
    @Column(name = "actor_role", length = 50)
    private String actorRole;

    // ── Action ─────────────────────────────────────────────────────────────

    /** Nature de l'action : CREATE, UPDATE, DELETE, STATUS_CHANGE, ASSIGN, UNASSIGN. */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private AuditAction action;

    // ── Entite cible ────────────────────────────────────────────────────────

    /** Type de l'entite concernee : PROJECT, TASK, PORTFOLIO, USER, ASSIGNMENT, DEPENDENCY. */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /** Identifiant de l'entite concernee. */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /** Nom lisible de l'entite au moment de l'action — copie denormalisee. */
    @Column(name = "entity_name", length = 255)
    private String entityName;

    /** Details lisibles de l'action (ex: "Status changed from NOT_STARTED to IN_PROGRESS"). */
    @Column(name = "details", length = 2000)
    private String details;

    // ── Contexte projet ─────────────────────────────────────────────────────

    /** Identifiant du projet de contexte (pour le filtrage par projet). */
    @Column(name = "project_id")
    private Long projectId;

    /** Nom du projet de contexte — copie denormalisee. */
    @Column(name = "project_name", length = 255)
    private String projectName;

    // ── Horodatage ──────────────────────────────────────────────────────────

    /** Timestamp de l'action — immutable une fois persiste. */
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}