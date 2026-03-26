package org.pfe.ppm_project.repositories;

import org.pfe.ppm_project.entities.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

    List<TaskDependency> findBySuccessor_Id(Long taskId);

    List<TaskDependency> findByPredecessor_Id(Long taskId);

    boolean existsByPredecessor_IdAndSuccessor_Id(Long predecessorId, Long successorId);

    // optionnel (anti-cycle plus tard)
    List<TaskDependency> findByPredecessor_IdAndSuccessor_Id(Long predecessorId, Long successorId);
}