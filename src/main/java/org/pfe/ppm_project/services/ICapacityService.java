package org.pfe.ppm_project.services;

import org.pfe.ppm_project.dto.AssignmentCapacityCheckDTO;
import org.pfe.ppm_project.dto.ResourceCapacityDTO;
import org.pfe.ppm_project.dto.WeeklyCapacityDTO;

import java.util.List;

public interface ICapacityService {

    /**
     * Capacity/workload overview for all active users.
     *
     * @param role      optional role filter (e.g. "DEV") — null = all roles
     * @param projectId optional project filter — null = all projects
     * @return list sorted by utilization % descending
     */
    List<ResourceCapacityDTO> getCapacityOverview(String role, Long projectId);

    /**
     * Week-by-week workload for each active user over a sliding window.
     *
     * @param weeks number of weeks to include (default 12 when null)
     * @param role  optional role filter
     */
    List<WeeklyCapacityDTO> getWeeklyCapacity(Integer weeks, String role);

    /**
     * Checks whether assigning {@code assignedHours} to a user on a given task
     * would cause a weekly capacity overload.
     *
     * The task's startDate→endDate range is split into calendar weeks.
     * For each week the service computes:
     *   - hours already assigned to the user (from other active tasks)
     *   - the new task's proportional contribution to that week
     *   - projected totals and availability
     *
     * @param userId        target user
     * @param taskId        task to be assigned
     * @param assignedHours hours the PM intends to allocate
     * @return week-by-week breakdown + global canAssign / hasOverload / message
     */
    AssignmentCapacityCheckDTO checkAssignmentCapacity(Long userId, Long taskId, Integer assignedHours);
}
