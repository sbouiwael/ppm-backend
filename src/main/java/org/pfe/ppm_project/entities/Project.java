package org.pfe.ppm_project.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entite JPA representant un projet dans le systeme de gestion de portefeuille.
 * Contient les informations principales du projet, ses dates, son avancement,
 * ainsi que les liens vers le chef de projet, le portefeuille et le calendrier de travail.
 */
@Entity
@Table(name = "projects")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Project {

    // Identifiant unique auto-genere
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === Champs principaux ===

    // Nom du projet (obligatoire)
    @Column(nullable = false)
    private String name;

    // Description du projet (max 2000 caracteres)
    @Column(length = 2000)
    private String description;

    // Date de debut du projet (obligatoire)
    @Column(nullable = false)
    private LocalDate startDate;

    // Date de fin prevue du projet
    private LocalDate endDate;

    // Indique si le projet est actif (suppression logique)
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // Chef de projet (relation Many-To-One, chargement paresseux, nullable si le PM est supprime)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_manager_id")
    private User projectManager;

    // Portefeuille auquel appartient le projet (optionnel)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portefeuille_id")
    @JsonIgnoreProperties({"projects"}) // Evite la recursion infinie lors de la serialisation JSON
    private Portefeuille portefeuille;

    // === Champs lies au portefeuille (optionnels) ===

    // Nom du portefeuille (champ texte libre)
    private String portfolioName;
    // Nom du programme
    private String programName;
    // Nom du sous-programme
    private String subProgramName;
    // Objectif strategique du projet
    private String objective;

    // === Calendrier (optionnel) ===

    // Nom du calendrier utilise (champ texte libre)
    private String calendarName;

    // Reference vers le calendrier de travail (jours ouvres, exceptions)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id")
    private WorkCalendar calendar;

    // === Dates de reference (baseline) pour le suivi des ecarts ===
    private LocalDate baselineStartDate;
    private LocalDate baselineEndDate;

    // Pourcentage d'avancement du projet (0-100), utile pour le tableau de bord
    @Builder.Default
    @Column(nullable = false)
    private Integer progress = 0;

    // Date de creation (remplie automatiquement, non modifiable)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Date de derniere modification
    private LocalDateTime updatedAt;

    // Callback JPA : initialise les dates avant la premiere sauvegarde
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        normalize();
    }

    // Callback JPA : met a jour la date de modification avant chaque mise a jour
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        normalize();
    }

    @Override
    public String toString() {
        return "Project{id=" + id + ", name='" + name + "', progress=" + progress + "}";
    }

    // Normalise les champs : trim des textes, clamp du progres, coherence des dates
    private void normalize() {
        if (this.name != null) this.name = this.name.trim();
        if (this.description != null) this.description = this.description.trim();

        // Le progres doit rester entre 0 et 100
        if (this.progress == null || this.progress < 0) this.progress = 0;
        if (this.progress > 100) this.progress = 100;

        // La date de fin ne peut pas preceder la date de debut
        if (this.startDate != null && this.endDate != null && this.endDate.isBefore(this.startDate)) {
            this.endDate = this.startDate;
        }

        // Coherence des dates baseline
        if (this.baselineStartDate != null && this.baselineEndDate != null
                && this.baselineEndDate.isBefore(this.baselineStartDate)) {
            this.baselineEndDate = this.baselineStartDate;
        }
    }
}
