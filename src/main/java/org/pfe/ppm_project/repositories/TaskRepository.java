package org.pfe.ppm_project.repositories;

import org.pfe.ppm_project.entities.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);
    List<Task> findByProjectIdOrderBySortOrderAsc(Long projectId);
    List<Task> findByParentTaskId(Long parentTaskId);
}
