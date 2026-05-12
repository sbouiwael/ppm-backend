package org.pfe.ppm_project.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entite JPA representant une exception dans un calendrier de travail.
 * Permet de definir des jours feries (non travailles) ou des jours
 * exceptionnellement travailles (ex: samedi ouvre).
 */
@Entity
@Table(name = "calendar_exceptions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CalendarException {

    // Identifiant unique auto-genere
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Calendrier auquel appartient cette exception (ignore en JSON pour eviter la recursion)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_id", nullable = false)
    @JsonIgnore
    private WorkCalendar calendar;

    // Date de l'exception
    @Column(nullable = false)
    private LocalDate date;

    // Nom/libelle de l'exception (ex: "Aid El Fitr", "Samedi exceptionnel")
    @Column(nullable = false)
    private String name;

    // true = jour exceptionnellement travaille (ex: samedi), false = jour ferie (non travaille)
    @Builder.Default
    @Column(nullable = false)
    private boolean working = false;

    // Nombre d'heures de travail pour ce jour d'exception (0 si non travaille)
    @Builder.Default
    @Column(nullable = false)
    private Double workHours = 0.0;

    @Override
    public String toString() {
        return "CalendarException{id=" + id + ", date=" + date + ", name='" + name + "', working=" + working + "}";
    }
}
