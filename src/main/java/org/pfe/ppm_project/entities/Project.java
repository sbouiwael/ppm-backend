package org.pfe.ppm_project.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === Core (existing) ===
    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_manager_id", nullable = false)
    private User projectManager;

    // === Portfolio fields (new, optional) ===
    private String portfolioName;
    private String programName;
    private String subProgramName;
    private String objective;

    // Calendar (optional)
    private String calendarName;

    // Baseline (optional)
    private LocalDate baselineStartDate;
    private LocalDate baselineEndDate;

    // Progress / status at project level (optional, useful for dashboard)
    @Builder.Default
    @Column(nullable = false)
    private Integer progress = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        normalize();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        normalize();
    }

    private void normalize() {
        if (this.name != null) this.name = this.name.trim();
        if (this.description != null) this.description = this.description.trim();

        if (this.progress == null || this.progress < 0) this.progress = 0;
        if (this.progress > 100) this.progress = 100;

        // Optional: enforce endDate >= startDate if both provided
        if (this.startDate != null && this.endDate != null && this.endDate.isBefore(this.startDate)) {
            this.endDate = this.startDate;
        }

        // Baseline consistency (optional)
        if (this.baselineStartDate != null && this.baselineEndDate != null
                && this.baselineEndDate.isBefore(this.baselineStartDate)) {
            this.baselineEndDate = this.baselineStartDate;
        }
    }
}