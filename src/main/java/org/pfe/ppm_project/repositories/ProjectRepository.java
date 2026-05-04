package org.pfe.ppm_project.repositories;

import org.pfe.ppm_project.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByProjectManagerId(Long projectManagerId);
    List<Project> findByActiveTrue();
    List<Project> findByPortefeuilleIsNull();

    @Modifying
    @Query("UPDATE Project p SET p.projectManager = null WHERE p.projectManager.id = :userId")
    void clearProjectManagerByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Project p SET p.portefeuille = null WHERE p.portefeuille.id = :portefeuilleId")
    void detachAllFromPortefeuille(@Param("portefeuilleId") Long portefeuilleId);
}
