package org.pfe.ppm_project.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entite JPA representant un calendrier de travail.
 * Definit les jours ouvres de la semaine, le nombre d'heures par jour
 * et les exceptions (jours feries, samedis exceptionnels, etc.).
 * Utilise pour calculer les dates de fin des taches en jours ouvres.
 */
@Entity
@Table(name = "work_calendars")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WorkCalendar {

    // Identifiant unique auto-genere
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nom unique du calendrier (ex: "Standard", "Calendrier Tunisie")
    @Column(nullable = false, unique = true)
    private String name;

    // Description du calendrier
    @Column(length = 500)
    private String description;

    // Nombre d'heures de travail par jour (par defaut 8h)
    @Builder.Default
    @Column(nullable = false)
    private Double workHoursPerDay = 8.0;

    /** Jours ouvres, separes par des virgules : 1=LUNDI .. 7=DIMANCHE. Ex: "1,2,3,4,5" pour Lun-Ven */
    @Builder.Default
    @Column(nullable = false)
    private String workDays = "1,2,3,4,5";

    // Liste des exceptions du calendrier (jours feries, jours ouvres exceptionnels)
    // CascadeType.ALL + orphanRemoval : les exceptions sont gerees en totalite par le calendrier
    @OneToMany(mappedBy = "calendar", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CalendarException> exceptions = new ArrayList<>();

    // Date de creation (non modifiable)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Callback JPA : initialise la date de creation
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** Returns an unmodifiable view — use addException() to append. */
    public List<CalendarException> getExceptions() {
        return Collections.unmodifiableList(exceptions);
    }

    /** Adds an exception to the managed collection (used by seeders/builders). */
    public void addException(CalendarException ex) {
        exceptions.add(ex);
    }

    /** Convertit la chaine workDays en liste de DayOfWeek pour faciliter les calculs */
    public List<DayOfWeek> getWorkDaysList() {
        List<DayOfWeek> days = new ArrayList<>();
        if (workDays == null || workDays.isBlank()) return days;
        for (String s : workDays.split(",")) {
            try {
                int val = Integer.parseInt(s.trim());
                if (val >= 1 && val <= 7) {
                    days.add(DayOfWeek.of(val));
                }
            } catch (NumberFormatException ignored) {}
        }
        return days;
    }
}
