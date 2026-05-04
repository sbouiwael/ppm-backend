package org.pfe.ppm_project.dto;

import java.util.List;

/**
 * DTO for GET /api/capacity/weekly.
 * Breaks down each active user's assigned workload week by week
 * across a sliding window (default: current week + 11 = 12 weeks).
 *
 * Distribution formula per week:
 *   contribution = assignedHours * (daysOverlap / totalTaskDays)
 */
public record WeeklyCapacityDTO(

        Long userId,
        String firstName,
        String lastName,
        String role,
        Integer weeklyCapacity,

        /** Weeks in chronological order (oldest first). */
        List<WeekSlot> weeks

) {

    public record WeekSlot(

            /** Readable label — e.g. "W15 2026" */
            String weekLabel,

            /** ISO-8601 Monday of the week — e.g. "2026-04-07" */
            String weekStart,

            /** ISO-8601 Friday of the week — e.g. "2026-04-11" */
            String weekEnd,

            /** Hours assigned to this user in this week (sum of proportional task contributions) */
            double assignedHours,

            /** weeklyCapacity - assignedHours (negative = overload) */
            double availableHours,

            /** (assignedHours / weeklyCapacity) * 100 — 0.0 when weeklyCapacity = 0 */
            double utilizationPct,

            /**
             * OVERLOADED    : > 100%
             * BALANCED      : 75–100%
             * UNDERUTILIZED : < 75%
             * NO_CAPACITY   : weeklyCapacity = 0
             */
            String status

    ) {}
}
