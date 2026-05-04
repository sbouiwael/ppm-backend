package org.pfe.ppm_project.repositories;

import org.pfe.ppm_project.entities.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    List<TaskAssignment> findByTaskId(Long taskId);

    List<TaskAssignment> findByUserId(Long userId);

    @Query("""
        select coalesce(sum(ta.assignedHours),0)
        from TaskAssignment ta
        where ta.task.id = :taskId and ta.active = true
    """)
    int sumActiveAssignedHoursByTask(@Param("taskId") Long taskId);

    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    /**
     * Retourne les IDs des utilisateurs ACTIFS affectes a une tache.
     * Utilise en lecture rapide pour enrichir le TaskDTO sans charger les entites completes.
     */
    @Query("""
        select ta.user.id
        from TaskAssignment ta
        where ta.task.id = :taskId and ta.active = true
    """)
    List<Long> findActiveUserIdsByTaskId(@Param("taskId") Long taskId);

    /**
     * Trouve une affectation (active ou non) par tache et utilisateur.
     * Utilise pour la reactivation d'une affectation precedemment desactivee.
     */
    java.util.Optional<TaskAssignment> findByTaskIdAndUserId(Long taskId, Long userId);

    /**
     * Verifie qu'une affectation ACTIVE existe pour cette tache et cet utilisateur.
     * Distinct de existsByTaskIdAndUserId qui inclut les enregistrements inactifs.
     */
    @Query("""
        select count(ta) > 0
        from TaskAssignment ta
        where ta.task.id = :taskId and ta.user.id = :userId and ta.active = true
    """)
    boolean existsActiveByTaskIdAndUserId(@Param("taskId") Long taskId, @Param("userId") Long userId);

    @Query("""
select ta from TaskAssignment ta
join fetch ta.task
join fetch ta.user
where ta.task.id = :taskId and ta.active = true
""")
    List<TaskAssignment> findByTaskIdWithRefs(@Param("taskId") Long taskId);

    @Query("""
select ta from TaskAssignment ta
join fetch ta.task
join fetch ta.user
where ta.user.id = :userId
""")
    List<TaskAssignment> findByUserIdWithRefs(@Param("userId") Long userId);

    @Query("""
select coalesce(sum(ta.assignedHours),0)
from TaskAssignment ta
where ta.task.id = :taskId and ta.active = true and ta.id <> :excludeId
""")
    int sumActiveAssignedHoursByTaskExcluding(@Param("taskId") Long taskId, @Param("excludeId") Long excludeId);

    /**
     * Retourne la somme des heures assignees par utilisateur pour toutes les affectations actives.
     * Resultat : tableau [userId (Long), totalHours (Long)] — une ligne par utilisateur actif.
     * Utilise dans DashboardService pour eviter le N+1 (un seul appel SQL au lieu de N).
     */
    @Modifying
    @Query("DELETE FROM TaskAssignment ta WHERE ta.task.id IN :taskIds")
    void deleteByTaskIdIn(@Param("taskIds") List<Long> taskIds);

    @Query("""
        SELECT ta.user.id, COALESCE(SUM(ta.assignedHours), 0)
        FROM TaskAssignment ta
        WHERE ta.active = true
        GROUP BY ta.user.id
    """)
    List<Object[]> sumActiveAssignedHoursByUser();

    /**
     * Recupere toutes les affectations actives d'un utilisateur avec les details de la tache et du projet.
     * Utilise JOIN FETCH pour eviter le N+1 : une seule requete SQL charge l'affectation,
     * la tache et le projet en une passe.
     *
     * Filtres appliques :
     * - affectation active (ta.active = true)
     * - tache active (t.active = true)
     * Logique Microsoft PPM : un contributeur ne voit que les taches sur lesquelles
     * il est explicitement affecte et qui sont encore en cours (non supprimees).
     */
    @Query("""
        SELECT ta FROM TaskAssignment ta
        JOIN FETCH ta.task t
        JOIN FETCH t.project p
        WHERE ta.user.id = :userId
          AND ta.active = true
          AND t.active = true
    """)
    List<TaskAssignment> findActiveAssignmentsByUserId(@Param("userId") Long userId);
}
