package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pfe.ppm_project.enums.NotificationType;
import org.pfe.ppm_project.repositories.TaskAssignmentRepository;
import org.pfe.ppm_project.repositories.TaskRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Planificateur de notifications de delais pour l'application PPM.
 *
 * RESPONSABILITES :
 *   1. Detecter quotidiennement les taches dont l'echeance approche (dans 3 jours)
 *      → envoie NotificationType.DEADLINE_APPROACHING a chaque utilisateur affecte.
 *   2. Detecter quotidiennement les taches en retard (endDate depassee, non terminees)
 *      → envoie NotificationType.TASK_OVERDUE a chaque utilisateur affecte.
 *
 * QUAND :
 *   Tourne tous les matins ouvres (lun-ven) a 8h00.
 *   Configurable via application.properties : app.scheduler.deadline-cron.
 *
 * ANTI-SPAM :
 *   NotificationService.createNotification() deduplique automatiquement :
 *   si une notification non lue du meme type+entite existe deja, elle n'est pas recree.
 *   Cela empeche d'inonder l'utilisateur de rappels repetes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeadlineNotificationScheduler {

    /** Nombre de jours avant echeance pour declencher une alerte DEADLINE_APPROACHING */
    private static final int DEADLINE_WARNING_DAYS = 3;

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final INotificationService notificationService;

    /**
     * Verifie quotidiennement les delais et envoie les notifications appropriees.
     * Tourne a 8h00 du lundi au vendredi.
     */
    @Scheduled(cron = "0 0 8 * * MON-FRI")
    @Transactional(readOnly = true)
    public void checkDeadlines() {
        LocalDate today = LocalDate.now();
        LocalDate warningThreshold = today.plusDays(DEADLINE_WARNING_DAYS);

        log.info("[DEADLINE-SCHEDULER] Running deadline check for date={}", today);

        notifyApproachingDeadlines(today, warningThreshold);
        notifyOverdueTasks(today);
    }

    // ─── Echeances approchantes ───

    private void notifyApproachingDeadlines(LocalDate today, LocalDate threshold) {
        var tasks = taskRepository.findTasksApproachingDeadline(today, threshold);

        log.info("[DEADLINE-SCHEDULER] {} task(s) approaching deadline (within {} days)",
                tasks.size(), DEADLINE_WARNING_DAYS);

        for (var task : tasks) {
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, task.getEndDate());
            String title = "Deadline approaching: " + task.getName();
            String message = "Task '" + task.getName() + "'"
                    + " in project '" + task.getProject().getName() + "'"
                    + " is due in " + daysLeft + " day(s) (" + task.getEndDate() + ")."
                    + " Current status: " + task.getStatus() + ".";

            // Notifie chaque utilisateur affecte a cette tache
            taskAssignmentRepository.findActiveUserIdsByTaskId(task.getId())
                    .forEach(userId -> {
                        try {
                            notificationService.createNotification(
                                    userId,
                                    NotificationType.DEADLINE_APPROACHING,
                                    title,
                                    message,
                                    "TASK",
                                    task.getId()
                            );
                        } catch (Exception e) {
                            log.warn("[DEADLINE-SCHEDULER] Failed to create DEADLINE_APPROACHING notification "
                                    + "for userId={} taskId={}: {}", userId, task.getId(), e.getMessage());
                        }
                    });
        }
    }

    // ─── Taches en retard ───

    private void notifyOverdueTasks(LocalDate today) {
        var tasks = taskRepository.findOverdueTasks(today);

        log.info("[DEADLINE-SCHEDULER] {} overdue task(s) found", tasks.size());

        for (var task : tasks) {
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(task.getEndDate(), today);
            String title = "Overdue task: " + task.getName();
            String message = "Task '" + task.getName() + "'"
                    + " in project '" + task.getProject().getName() + "'"
                    + " was due on " + task.getEndDate()
                    + " (" + daysOverdue + " day(s) overdue)."
                    + " Current status: " + task.getStatus() + ".";

            taskAssignmentRepository.findActiveUserIdsByTaskId(task.getId())
                    .forEach(userId -> {
                        try {
                            notificationService.createNotification(
                                    userId,
                                    NotificationType.TASK_OVERDUE,
                                    title,
                                    message,
                                    "TASK",
                                    task.getId()
                            );
                        } catch (Exception e) {
                            log.warn("[DEADLINE-SCHEDULER] Failed to create TASK_OVERDUE notification "
                                    + "for userId={} taskId={}: {}", userId, task.getId(), e.getMessage());
                        }
                    });
        }
    }
}