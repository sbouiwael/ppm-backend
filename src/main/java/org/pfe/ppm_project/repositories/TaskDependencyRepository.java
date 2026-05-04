package org.pfe.ppm_project.repositories;

import org.pfe.ppm_project.entities.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

    // ── Eager-loading queries (JOIN FETCH both sides) ──────────────────────────
    // Used wherever task names must be available on the DTO.
    // Replaces plain derived queries to eliminate N+1 lazy-load hits and avoid
    // any risk of LazyInitializationException outside the Hibernate session.

    @Query("""
        select td from TaskDependency td
        join fetch td.predecessor
        join fetch td.successor
        where td.successor.id = :taskId
        """)
    List<TaskDependency> findBySuccessor_Id(@Param("taskId") Long taskId);

    @Query("""
        select td from TaskDependency td
        join fetch td.predecessor
        join fetch td.successor
        where td.predecessor.id = :taskId
        """)
    List<TaskDependency> findByPredecessor_Id(@Param("taskId") Long taskId);

    @Query("""
        select td from TaskDependency td
        join fetch td.predecessor
        join fetch td.successor
        where td.predecessor.project.id = :projectId
        """)
    List<TaskDependency> findByPredecessor_Project_Id(@Param("projectId") Long projectId);

    // ── Existence check (no entity graph needed — only IDs used) ──────────────
    boolean existsByPredecessor_IdAndSuccessor_Id(Long predecessorId, Long successorId);

    List<TaskDependency> findByPredecessor_IdAndSuccessor_Id(Long predecessorId, Long successorId);

    @Modifying
    @Query("DELETE FROM TaskDependency d WHERE d.predecessor.id IN :taskIds OR d.successor.id IN :taskIds")
    void deleteByTaskIds(@Param("taskIds") List<Long> taskIds);
}
