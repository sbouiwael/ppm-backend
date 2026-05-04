package org.pfe.ppm_project.controller;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.AssignmentCapacityCheckDTO;
import org.pfe.ppm_project.dto.ResourceCapacityDTO;
import org.pfe.ppm_project.dto.WeeklyCapacityDTO;
import org.pfe.ppm_project.services.ICapacityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Capacity planning endpoints.
 *
 * RBAC:
 *   ADMIN / PMO / PM / RH — full read access (resource planning)
 *   DEV / QA / DEVOPS      — no access here; see /api/assignments/me for personal workload
 */
@RestController
@RequestMapping("/api/capacity")
@RequiredArgsConstructor
public class CapacityController {

    private final ICapacityService capacityService;

    /**
     * Consolidated capacity/workload overview for all active resources.
     *
     * @param role      optional role filter — e.g. DEV, QA, DEVOPS
     * @param projectId optional project filter (limits assigned hours to that project)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM','RH')")
    public ResponseEntity<List<ResourceCapacityDTO>> getCapacityOverview(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long projectId) {

        return ResponseEntity.ok(capacityService.getCapacityOverview(role, projectId));
    }

    /**
     * Week-by-week workload heatmap over a sliding window.
     *
     * @param weeks number of weeks to include (1–52, default 12)
     * @param role  optional role filter
     */
    @GetMapping("/weekly")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM','RH')")
    public ResponseEntity<List<WeeklyCapacityDTO>> getWeeklyCapacity(
            @RequestParam(defaultValue = "12") Integer weeks,
            @RequestParam(required = false) String role) {

        return ResponseEntity.ok(capacityService.getWeeklyCapacity(weeks, role));
    }

    /**
     * Pre-assignment capacity check — week-by-week impact analysis.
     *
     * Evaluates whether assigning {@code assignedHours} to {@code userId} on
     * {@code taskId} would cause a capacity overload in any scheduled week.
     * The response always includes {@code canAssign = true} when dates are present
     * (the PM decides); {@code hasOverload} signals whether any week exceeds
     * the user's weekly capacity.
     *
     * @param userId        ID of the user to be assigned
     * @param taskId        ID of the task
     * @param assignedHours hours the PM intends to allocate
     */
    @GetMapping("/check-assignment")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM','RH')")
    public ResponseEntity<AssignmentCapacityCheckDTO> checkAssignment(
            @RequestParam Long userId,
            @RequestParam Long taskId,
            @RequestParam Integer assignedHours) {

        return ResponseEntity.ok(
                capacityService.checkAssignmentCapacity(userId, taskId, assignedHours));
    }
}
