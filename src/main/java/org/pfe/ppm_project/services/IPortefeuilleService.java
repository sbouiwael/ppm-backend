package org.pfe.ppm_project.services;

import org.pfe.ppm_project.dto.PortefeuilleCreateUpdateDTO;
import org.pfe.ppm_project.dto.PortefeuilleDTO;
import org.pfe.ppm_project.dto.ProjectDTO;

import java.util.List;
import java.util.Optional;

public interface IPortefeuilleService {

    Optional<PortefeuilleDTO> create(PortefeuilleCreateUpdateDTO dto);

    List<PortefeuilleDTO> getAll();

    Optional<PortefeuilleDTO> getById(Long id);

    Optional<PortefeuilleDTO> update(Long id, PortefeuilleCreateUpdateDTO dto);

    boolean delete(Long id);

    Optional<PortefeuilleDTO> addProject(Long portefeuilleId, Long projectId);

    Optional<PortefeuilleDTO> removeProject(Long portefeuilleId, Long projectId);

    List<ProjectDTO> getUnassignedProjects();
}
