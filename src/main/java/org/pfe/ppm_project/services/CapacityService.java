package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.AssignmentCapacityCheckDTO;
import org.pfe.ppm_project.dto.ResourceCapacityDTO;
import org.pfe.ppm_project.dto.WeeklyCapacityDTO;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.entities.TaskAssignment;
import org.pfe.ppm_project.entities.User;
import org.pfe.ppm_project.exception.ResourceNotFoundException;
import org.pfe.ppm_project.repositories.TaskAssignmentRepository;
import org.pfe.ppm_project.repositories.TaskRepository;
import org.pfe.ppm_project.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CapacityService implements ICapacityService {

    private final UserRepository             userRepository;
    private final TaskRepository             taskRepository;
    private final TaskAssignmentRepository   taskAssignmentRepository;

    // ── Overview ──────────────────────────────────────────────────────────────

    @Override
    public List<ResourceCapacityDTO> getCapacityOverview(String role, Long projectId) {
        List<User> users = userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> role == null || role.isBlank()
                        || u.getRole().name().equalsIgnoreCase(role))
                .collect(Collectors.toList());

        return users.stream()
                .map(u -> buildCapacityDTO(u, projectId))
                .sorted(Comparator.comparingDouble(ResourceCapacityDTO::utilizationPct).reversed())
                .collect(Collectors.toList());
    }

    private ResourceCapacityDTO buildCapacityDTO(User user, Long projectId) {
        List<TaskAssignment> assignments = taskAssignmentRepository.findByUserIdWithRefs(user.getId());

        int totalAssignedHours = assignments.stream()
                .filter(TaskAssignment::isActive)
                .filter(ta -> ta.getTask() != null && ta.getTask().isActive())
                .filter(ta -> projectId == null
                        || (ta.getTask().getProject() != null
                            && projectId.equals(ta.getTask().getProject().getId())))
                .mapToInt(TaskAssignment::getAssignedHours)
                .sum();

        int capacity = user.getWeeklyCapacity() != null ? user.getWeeklyCapacity() : 0;
        double utilizationPct = capacity > 0
                ? (double) totalAssignedHours / capacity * 100.0
                : 0.0;
        String status = computeOverviewStatus(capacity, utilizationPct);

        return new ResourceCapacityDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                capacity,
                totalAssignedHours,
                Math.round(utilizationPct * 10.0) / 10.0,
                status
        );
    }

    /**
     * Status thresholds for the overview and weekly heatmap endpoints.
     * Kept as OVERLOADED / BALANCED / UNDERUTILIZED / NO_CAPACITY for backwards compatibility.
     */
    private String computeOverviewStatus(int weeklyCapacity, double utilizationPct) {
        if (weeklyCapacity <= 0)    return "NO_CAPACITY";
        if (utilizationPct > 100.0) return "OVERLOADED";
        if (utilizationPct >= 75.0) return "BALANCED";
        return "UNDERUTILIZED";
    }

    // ── Weekly Heatmap ────────────────────────────────────────────────────────

    @Override
    public List<WeeklyCapacityDTO> getWeeklyCapacity(Integer weeksParam, String role) {
        int numWeeks = (weeksParam != null && weeksParam > 0) ? Math.min(weeksParam, 52) : 12;

        LocalDate windowStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<LocalDate> weekStarts = new ArrayList<>();
        for (int i = 0; i < numWeeks; i++) {
            weekStarts.add(windowStart.plusWeeks(i));
        }

        List<User> users = userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> role == null || role.isBlank()
                        || u.getRole().name().equalsIgnoreCase(role))
                .sorted(Comparator.comparing((User u) -> u.getRole().name())
                        .thenComparing(User::getLastName))
                .collect(Collectors.toList());

        return users.stream()
                .map(u -> buildWeeklyDTO(u, weekStarts))
                .collect(Collectors.toList());
    }

    private WeeklyCapacityDTO buildWeeklyDTO(User user, List<LocalDate> weekStarts) {
        List<TaskAssignment> assignments = taskAssignmentRepository.findByUserIdWithRefs(user.getId())
                .stream()
                .filter(TaskAssignment::isActive)
                .filter(ta -> ta.getTask() != null && ta.getTask().isActive())
                .collect(Collectors.toList());

        int capacity = user.getWeeklyCapacity() != null ? user.getWeeklyCapacity() : 0;

        List<WeeklyCapacityDTO.WeekSlot> slots = weekStarts.stream()
                .map(ws -> buildWeekSlot(ws, assignments, capacity))
                .collect(Collectors.toList());

        return new WeeklyCapacityDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                capacity,
                slots
        );
    }

    private WeeklyCapacityDTO.WeekSlot buildWeekSlot(
            LocalDate weekStart,
            List<TaskAssignment> assignments,
            int capacity
    ) {
        LocalDate weekEnd = weekStart.plusDays(4); // Friday

        double hoursThisWeek = assignments.stream()
                .mapToDouble(ta -> proportionalHours(ta, weekStart, weekEnd))
                .sum();

        double rounded        = Math.round(hoursThisWeek * 10.0) / 10.0;
        double availableHours = Math.round((capacity - hoursThisWeek) * 10.0) / 10.0;
        double utilPct        = capacity > 0
                ? Math.round(hoursThisWeek / capacity * 1000.0) / 10.0
                : 0.0;
        String status         = computeOverviewStatus(capacity, utilPct);

        int weekNum   = weekStart.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year      = weekStart.get(IsoFields.WEEK_BASED_YEAR);
        String label  = String.format("W%02d %d", weekNum, year);

        return new WeeklyCapacityDTO.WeekSlot(
                label,
                weekStart.toString(),
                weekEnd.toString(),
                rounded,
                availableHours,
                utilPct,
                status
        );
    }

    // ── Assignment Capacity Check ─────────────────────────────────────────────

    @Override
    public AssignmentCapacityCheckDTO checkAssignmentCapacity(Long userId, Long taskId, Integer assignedHours) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        String userName = user.getFirstName() + " " + user.getLastName();

        if (!user.isActive()) {
            return new AssignmentCapacityCheckDTO(userId, userName, taskId, task.getName(),
                    List.of(), false, false, "User is not active.");
        }

        if (task.getStartDate() == null || task.getEndDate() == null) {
            return new AssignmentCapacityCheckDTO(userId, userName, taskId, task.getName(),
                    List.of(), false, false,
                    "Task is missing start/end dates. Please set dates before checking capacity.");
        }

        int capacity = user.getWeeklyCapacity() != null ? user.getWeeklyCapacity() : 0;
        int hours    = assignedHours != null ? Math.max(0, assignedHours) : 0;

        // Weeks covered by the task (one entry per calendar week: Monday → Friday)
        LocalDate firstWeekStart = task.getStartDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<LocalDate> taskWeeks = new ArrayList<>();
        LocalDate cursor = firstWeekStart;
        while (!cursor.isAfter(task.getEndDate())) {
            taskWeeks.add(cursor);
            cursor = cursor.plusWeeks(1);
        }

        // Existing active assignments for this user, excluding the task being tested
        List<TaskAssignment> existing = taskAssignmentRepository.findByUserIdWithRefs(userId)
                .stream()
                .filter(TaskAssignment::isActive)
                .filter(ta -> ta.getTask() != null && ta.getTask().isActive())
                .filter(ta -> !ta.getTask().getId().equals(taskId))
                .collect(Collectors.toList());

        // Use business days so weekend days don't silently absorb a share of the
        // assigned hours that would never appear in any Mon–Fri week window.
        long taskDays = Math.max(1, businessDaysBetween(task.getStartDate(), task.getEndDate()));

        List<AssignmentCapacityCheckDTO.WeekCapacityDetail> weekDetails = taskWeeks.stream()
                .map(ws -> buildCheckDetail(ws, existing, task, hours, capacity, taskDays))
                .collect(Collectors.toList());

        boolean hasOverload = weekDetails.stream().anyMatch(w -> w.overloadHours() > 0);
        long overloadedWeeks = weekDetails.stream().filter(w -> w.overloadHours() > 0).count();
        double totalOverload  = weekDetails.stream().mapToDouble(AssignmentCapacityCheckDTO.WeekCapacityDetail::overloadHours).sum();

        String message;
        if (capacity <= 0) {
            message = "This user has no configured weekly capacity. Please set weeklyCapacity first.";
        } else if (hasOverload) {
            message = String.format(
                    "Warning: assigning %dh will cause overload in %d week(s). Total excess: %.1fh.",
                    hours, overloadedWeeks, totalOverload);
        } else {
            message = "This user has enough capacity for this task across all scheduled weeks.";
        }

        return new AssignmentCapacityCheckDTO(
                userId, userName, taskId, task.getName(),
                weekDetails,
                true,   // canAssign — PM decides; UI shows warning but keeps button enabled
                hasOverload,
                message
        );
    }

    private AssignmentCapacityCheckDTO.WeekCapacityDetail buildCheckDetail(
            LocalDate weekStart,
            List<TaskAssignment> existingAssignments,
            Task newTask,
            int newHours,
            int capacity,
            long taskDays
    ) {
        LocalDate weekEnd = weekStart.plusDays(4);

        // Already committed hours in this week (from other tasks)
        double alreadyAssigned = existingAssignments.stream()
                .mapToDouble(ta -> proportionalHours(ta, weekStart, weekEnd))
                .sum();

        // Contribution of the new assignment to this week
        LocalDate taskStart   = newTask.getStartDate();
        LocalDate taskEnd     = newTask.getEndDate();
        LocalDate overlapStart = taskStart.isAfter(weekStart) ? taskStart : weekStart;
        LocalDate overlapEnd   = taskEnd.isBefore(weekEnd)   ? taskEnd   : weekEnd;

        double newTaskHours = 0.0;
        if (!overlapEnd.isBefore(overlapStart)) {
            long overlap = ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
            newTaskHours = (double) newHours * overlap / taskDays;
        }

        double projected      = alreadyAssigned + newTaskHours;
        double availableAfter = capacity - projected;
        double overloadHours  = Math.max(0.0, projected - capacity);
        double projectedUtil  = capacity > 0 ? projected / capacity * 100.0 : 0.0;

        String status;
        if (capacity <= 0) {
            status = "NO_CAPACITY";
        } else if (projected > capacity) {
            status = "OVERLOADED";
        } else if (projectedUtil >= 75.0) {
            status = "NEAR_CAPACITY";
        } else {
            status = "AVAILABLE";
        }

        return new AssignmentCapacityCheckDTO.WeekCapacityDetail(
                weekStart.toString(),
                weekEnd.toString(),
                capacity,
                round1(alreadyAssigned),
                round1(newTaskHours),
                round1(projected),
                round1(availableAfter),
                status,
                round1(overloadHours)
        );
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    /**
     * Proportional hours an assignment contributes to a given Mon–Fri week window.
     *
     * Formula: assignedHours × (weekdayOverlap / totalBusinessDays)
     *
     * WHY business days in the denominator:
     *   The week windows are always Mon–Fri (weekEnd = weekStart.plusDays(4)).
     *   Weekend calendar days therefore NEVER appear in any week's numerator.
     *   Using calendar days in the denominator would cause hours "allocated" to
     *   Sat/Sun to be silently discarded — e.g. a 15 h task over 9 calendar days
     *   (7 weekdays + 2 weekend days) would distribute only 11.7 h across weeks,
     *   losing 3.3 h into the weekend sink.  Business-day denominator fixes this.
     *
     * The numerator (overlapStart→overlapEnd) is always clipped to Mon–Fri so
     * ChronoUnit.DAYS correctly counts weekdays there without adjustment.
     */
    private double proportionalHours(TaskAssignment ta, LocalDate weekStart, LocalDate weekEnd) {
        Task task = ta.getTask();
        LocalDate start = task.getStartDate();
        LocalDate end   = task.getEndDate();
        if (start == null || end == null || end.isBefore(start)) return 0.0;

        LocalDate overlapStart = start.isAfter(weekStart) ? start : weekStart;
        LocalDate overlapEnd   = end.isBefore(weekEnd)   ? end   : weekEnd;
        if (overlapEnd.isBefore(overlapStart)) return 0.0;

        // Numerator: calendar-day distance is safe here because overlapStart/End
        // are both clipped to the Mon–Fri window — all days in range are weekdays.
        long overlap      = ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
        long taskBizDays  = Math.max(1, businessDaysBetween(start, end));
        int  hours        = ta.getAssignedHours() != null ? ta.getAssignedHours() : 0;
        return (double) hours * overlap / taskBizDays;
    }

    /**
     * Counts Mon–Fri business days between start and end (both inclusive).
     * Weekend days are excluded so the capacity distribution denominator
     * matches the Mon–Fri week windows used in the numerator.
     */
    private long businessDaysBetween(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) return 0;
        return start.datesUntil(end.plusDays(1))
                .filter(d -> {
                    DayOfWeek dow = d.getDayOfWeek();
                    return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
                })
                .count();
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
