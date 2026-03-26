package org.pfe.ppm_project.entities;

import jakarta.persistence.*;
import lombok.*;
import org.pfe.ppm_project.enums.DependencyType;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "task_dependencies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"predecessor_task_id", "successor_task_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tâche précédente
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predecessor_task_id", nullable = false)
    private Task predecessor;

    // Tâche suivante
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "successor_task_id", nullable = false)
    private Task successor;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DependencyType type = DependencyType.FS;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
