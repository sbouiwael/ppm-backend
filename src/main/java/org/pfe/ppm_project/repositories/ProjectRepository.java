package org.pfe.ppm_project.repositories;

import org.pfe.ppm_project.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByProjectManagerId(Long projectManagerId);
    List<Project> findByActiveTrue();

}
