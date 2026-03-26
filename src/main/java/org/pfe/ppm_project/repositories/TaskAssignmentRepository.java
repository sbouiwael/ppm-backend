package org.pfe.ppm_project.repositories;

import org.pfe.ppm_project.entities.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query("""
select ta from TaskAssignment ta
join fetch ta.task
join fetch ta.user
where ta.task.id = :taskId
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

}
