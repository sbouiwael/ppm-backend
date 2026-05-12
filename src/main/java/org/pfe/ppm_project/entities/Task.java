package org.pfe.ppm_project.entities;

import jakarta.persistence.*;
import lombok.*;
import org.pfe.ppm_project.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entite JPA representant une tache au sein d'un projet.
 * Gere les informations de planification (duree, dates, WBS),
 * le suivi d'avancement, les references baseline et la hierarchie parent-enfant.
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"project", "parentTask"})
public class Task {

    // Nombre d'heures de travail standard par jour (constante metier)
    public static final double HOURS_PER_DAY = 8.0;

    // Identifiant unique auto-genere
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nom de la tache (obligatoire)
    @Column(nullable = false)
    private String name;

    // Description detaillee de la tache (max 2000 caracteres)
    @Column(length = 2000)
    private String description;

    // Projet auquel appartient cette tache (obligatoire)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Tache parente pour la hierarchie WBS (optionnel, null = tache racine)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private Task parentTask;

    // === Champs pour le diagramme de Gantt ===

    // Numero WBS (Work Breakdown Structure) pour l'organisation hierarchique
    @Column(nullable = false)
    @Builder.Default
    private String wbsNumber = "1";

    // Mode de la tache (ex: "TASK", "MILESTONE")
    @Builder.Default
    private String mode = "TASK";

    // Duree estimee en jours ouvrables
    @Column(nullable = false)
    @Builder.Default
    private Double durationDays = 1.0;

    // Charge de travail estimee en heures
    @Column(nullable = false)
    @Builder.Default
    private Double workHours = HOURS_PER_DAY;

    // === Donnees de reference (baseline) pour mesurer les ecarts ===
    private Double baselineDurationDays;
    private LocalDate baselineStartDate;
    private LocalDate baselineEndDate;

    // Heures de travail reellement effectuees
    private Double actualWorkHours;
    // Nom du calendrier associe (champ texte libre)
    private String calendarName;

    // Ordre d'affichage dans la liste des taches
    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    // Date de debut de la tache
    private LocalDate startDate;
    // Date de fin de la tache
    private LocalDate endDate;

    // Statut courant de la tache (NOT_STARTED, IN_PROGRESS, DONE, BLOCKED)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.NOT_STARTED;

    // Pourcentage d'avancement (0-100)
    @Builder.Default
    @Column(nullable = false)
    private Integer progress = 0;

    // Indicateur de suppression logique
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // Date de creation (non modifiable apres insertion)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Date de derniere modification
    private LocalDateTime updatedAt;

    // Callback JPA : initialise les timestamps avant la premiere sauvegarde
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        normalize();
    }

    // Callback JPA : met a jour le timestamp avant chaque modification
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        normalize();
    }

    // Normalise et valide les champs : duree, heures, progres, dates
    private void normalize() {
        if (this.name != null) this.name = this.name.trim();
        if (this.description != null) this.description = this.description.trim();

        // La duree doit etre au minimum 1 jour
        if (this.durationDays == null || this.durationDays <= 0) {
            this.durationDays = 1.0;
        }

        // Si les heures ne sont pas definies, on les calcule a partir de la duree
        if (this.workHours == null || this.workHours < 0) {
            this.workHours = this.durationDays * HOURS_PER_DAY;
        }

        // Le progres doit rester entre 0 et 100
        if (this.progress == null || this.progress < 0) this.progress = 0;
        if (this.progress > 100) this.progress = 100;

        // Logique metier PPM : synchronisation automatique statut <-> progres
        // (coherence avec Microsoft Project : 100% = DONE, 0% NOT_STARTED = reste NOT_STARTED)
        if (this.progress != null && this.status != null) {
            if (this.progress == 100 && this.status != TaskStatus.DONE) {
                this.status = TaskStatus.DONE;
            } else if (this.progress > 0 && this.progress < 100 && this.status == TaskStatus.NOT_STARTED) {
                this.status = TaskStatus.IN_PROGRESS;
            } else if (this.progress == 0 && this.status == TaskStatus.DONE) {
                this.status = TaskStatus.NOT_STARTED;
            }
        }

        // Valeurs par defaut pour WBS et mode
        if (this.wbsNumber == null || this.wbsNumber.isBlank()) {
            this.wbsNumber = String.valueOf(this.id != null ? this.id : 1);
        }
        if (this.mode == null || this.mode.isBlank()) {
            this.mode = "TASK";
        }

        if (this.sortOrder == null || this.sortOrder < 0) {
            this.sortOrder = 0;
        }

        // Duree baseline ne peut pas etre negative
        if (this.baselineDurationDays != null && this.baselineDurationDays < 0) {
            this.baselineDurationDays = null;
        }
        if (this.actualWorkHours != null && this.actualWorkHours < 0) {
            this.actualWorkHours = 0.0;
        }

        // La date de fin ne peut pas preceder la date de debut
        if (this.startDate != null && this.endDate != null && this.endDate.isBefore(this.startDate)) {
            this.endDate = this.startDate;
        }

        // Le calcul de endDate a partir de durationDays est gere par TaskService
        // en utilisant le calendrier du projet (jours ouvres uniquement).
        // Fallback : si endDate est encore null et qu'aucun calendrier n'est utilise, addition simple.
        // (Cela couvre @PrePersist quand TaskService n'est pas implique)
    }
}
