package org.pfe.ppm_project.repositories;

import org.pfe.ppm_project.entities.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);
    List<Task> findByProjectIdOrderBySortOrderAsc(Long projectId);
    List<Task> findByParentTaskId(Long parentTaskId);

    /** Vérifie si une tâche a au moins un enfant (utilisé pour bloquer la suppression). */
    boolean existsByParentTaskId(Long parentTaskId);

    /** Compte les enfants ACTIFS d'une tâche parente. */
    long countByParentTaskIdAndActiveTrue(Long parentTaskId);

    /**
     * Taches dont l'echeance approche : actives, non terminees,
     * avec une endDate entre today (inclus) et deadline (inclus).
     * Utilise JOIN FETCH pour eviter le N+1 lors de l'acces au projet dans le scheduler.
     */
    @Query("""
        SELECT DISTINCT t FROM Task t
        JOIN FETCH t.project p
        WHERE t.active = true
          AND t.status <> 'DONE'
          AND t.endDate IS NOT NULL
          AND t.endDate >= :today
          AND t.endDate <= :deadline
        """)
    List<Task> findTasksApproachingDeadline(@Param("today") LocalDate today,
                                            @Param("deadline") LocalDate deadline);

    /**
     * Taches en retard : actives, non terminees,
     * dont la date de fin est deja passee (endDate < today).
     */
    @Query("""
        SELECT DISTINCT t FROM Task t
        JOIN FETCH t.project p
        WHERE t.active = true
          AND t.status <> 'DONE'
          AND t.endDate IS NOT NULL
          AND t.endDate < :today
        """)
    List<Task> findOverdueTasks(@Param("today") LocalDate today);
}
