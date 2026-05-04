package org.pfe.ppm_project.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entite JPA representant un portefeuille de projets.
 * Un portefeuille regroupe plusieurs projets pour faciliter
 * leur gestion et leur suivi au niveau strategique.
 */
@Entity
@Table(name = "portefeuilles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Portefeuille {

    // Identifiant unique auto-genere
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nom du portefeuille (obligatoire)
    @Column(nullable = false)
    private String nom;

    // Description du portefeuille (max 2000 caracteres)
    @Column(length = 2000)
    private String description;

    // Liste des projets rattaches a ce portefeuille (relation One-To-Many)
    @OneToMany(mappedBy = "portefeuille")
    @Builder.Default
    private List<Project> projects = new ArrayList<>();
}
