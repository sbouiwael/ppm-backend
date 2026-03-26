package org.pfe.ppm_project.entities;

import jakarta.persistence.*;
import lombok.*;
import org.pfe.ppm_project.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    public static final double HOURS_PER_DAY = 8.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private Task parentTask;

    // === Table/Gantt fields ===
    @Column(nullable = false)
    @Builder.Default
    private String wbsNumber = "1";

    @Builder.Default
    private String mode = "TASK";

    @Column(nullable = false)
    @Builder.Default
    private Double durationDays = 1.0;

    @Column(nullable = false)
    @Builder.Default
    private Double workHours = HOURS_PER_DAY;

    private Double baselineDurationDays;
    private LocalDate baselineStartDate;
    private LocalDate baselineEndDate;

    private Double actualWorkHours;
    private String calendarName;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    private LocalDate startDate;
    private LocalDate endDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.NOT_STARTED;

    @Builder.Default
    @Column(nullable = false)
    private Integer progress = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

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

        // duration
        if (this.durationDays == null || this.durationDays <= 0) {
            this.durationDays = 1.0;
        }

        // work: if null or negative, compute from duration
        if (this.workHours == null || this.workHours < 0) {
            this.workHours = this.durationDays * HOURS_PER_DAY;
        }

        // clamp progress
        if (this.progress == null || this.progress < 0) this.progress = 0;
        if (this.progress > 100) this.progress = 100;

        // wbs / mode defaults
        if (this.wbsNumber == null || this.wbsNumber.isBlank()) {
            this.wbsNumber = String.valueOf(this.id != null ? this.id : 1);
        }
        if (this.mode == null || this.mode.isBlank()) {
            this.mode = "TASK";
        }

        if (this.sortOrder == null || this.sortOrder < 0) {
            this.sortOrder = 0;
        }

        // baseline duration sanity
        if (this.baselineDurationDays != null && this.baselineDurationDays < 0) {
            this.baselineDurationDays = null;
        }
        if (this.actualWorkHours != null && this.actualWorkHours < 0) {
            this.actualWorkHours = 0.0;
        }

        // dates consistency
        if (this.startDate != null && this.endDate != null && this.endDate.isBefore(this.startDate)) {
            this.endDate = this.startDate;
        }

        // If startDate exists and endDate missing, compute endDate from durationDays
        if (this.startDate != null && this.endDate == null) {
            long days = (long) Math.ceil(this.durationDays);
            if (days < 1) days = 1;
            this.endDate = this.startDate.plusDays(days);
        }

        // If startDate & endDate exist, optionally keep duration consistent
        // (we don't override durationDays automatically to avoid unexpected changes)
    }
}