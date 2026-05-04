package org.pfe.ppm_project.dto;

import java.util.List;

/**
 * Result of GET /api/capacity/check-assignment.
 * Breaks down the capacity impact of a proposed task assignment week by week,
 * so PMs can make informed decisions before committing an assignment.
 */
public record AssignmentCapacityCheckDTO(

        Long userId,
        String userName,

        Long taskId,
        String taskName,

        /** One entry per calendar week covered by the task's date range. */
        List<WeekCapacityDetail> weeks,

        /** Always true — the PM decides. False only on missing data (no task dates). */
        boolean canAssign,

        /** True if at least one week exceeds the user's weekly capacity. */
        boolean hasOverload,

        /** Human-readable summary for display in the UI. */
        String message

) {

    /**
     * Capacity breakdown for a single calendar week.
     */
    public record WeekCapacityDetail(

            /** ISO-8601 Monday date of the week — e.g. "2026-05-04" */
            String weekStart,

            /** ISO-8601 Friday date of the week — e.g. "2026-05-08" */
            String weekEnd,

            /** User's weekly capacity in hours */
            int weeklyCapacity,

            /** Hours already assigned to this user in this week (from other active tasks) */
            double alreadyAssignedHours,

            /** Proportional share of the new assignment's hours falling in this week */
            double newTaskHoursForWeek,

            /** alreadyAssignedHours + newTaskHoursForWeek */
            double projectedAssignedHours,

            /** weeklyCapacity - projectedAssignedHours (negative = overload) */
            double availableHoursAfterAssignment,

            /** AVAILABLE | NEAR_CAPACITY | OVERLOADED | NO_CAPACITY */
            String status,

            /** Max(0, projectedAssignedHours - weeklyCapacity) */
            double overloadHours

    ) {}
}
