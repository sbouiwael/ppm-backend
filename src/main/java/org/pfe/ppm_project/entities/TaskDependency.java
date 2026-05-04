package org.pfe.ppm_project.entities;

import jakarta.persistence.*;
import lombok.*;
import org.pfe.ppm_project.enums.DependencyType;

import java.time.LocalDateTime;

/**
 * Entite JPA representant une dependance entre deux taches.
 * Modelise les liens de type predecesseur/successeur utilises
 * dans le diagramme de Gantt (FS, SS, FF, SF).
 * La contrainte d'unicite empeche les doublons entre deux memes taches.
 */
@Entity
@Table(
        name = "task_dependencies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"predecessor_task_id", "successor_task_id"}) // Empeche les dependances en double
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDependency {

    // Identifiant unique auto-genere
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tache predecesseur (doit se terminer/commencer avant le successeur)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predecessor_task_id", nullable = false)
    private Task predecessor;

    // Tache successeur (depend du predecesseur)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "successor_task_id", nullable = false)
    private Task successor;

    // Type de dependance : FS (Fin-Debut), SS (Debut-Debut), FF (Fin-Fin), SF (Debut-Fin)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DependencyType type = DependencyType.FS;

    // Date de creation de la dependance (non modifiable)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Callback JPA : initialise la date de creation
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
