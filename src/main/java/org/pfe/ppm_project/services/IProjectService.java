package org.pfe.ppm_project.services;

import org.pfe.ppm_project.dto.ProjectCreateUpdateDTO;
import org.pfe.ppm_project.dto.ProjectDTO;

import java.util.List;
import java.util.Optional;

public interface IProjectService {

    Optional<ProjectDTO> create(ProjectCreateUpdateDTO dto);

    List<ProjectDTO> getAll();

    Optional<ProjectDTO> getById(Long id);

    List<ProjectDTO> getByManager(Long managerId);

    Optional<ProjectDTO> update(Long id, ProjectCreateUpdateDTO dto);

    boolean deactivate(Long id);
}
