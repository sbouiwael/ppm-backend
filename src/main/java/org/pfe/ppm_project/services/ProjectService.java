package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.ProjectCreateUpdateDTO;
import org.pfe.ppm_project.dto.ProjectDTO;
import org.pfe.ppm_project.entities.Project;
import org.pfe.ppm_project.entities.User;
import org.pfe.ppm_project.repositories.ProjectRepository;
import org.pfe.ppm_project.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectService implements IProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    public Optional<ProjectDTO> create(ProjectCreateUpdateDTO dto) {
        if (dto == null || dto.name() == null || dto.name().isBlank()
                || dto.startDate() == null || dto.projectManagerId() == null) {
            return Optional.empty();
        }

        User pm = userRepository.findById(dto.projectManagerId()).orElse(null);
        if (pm == null) return Optional.empty();

        Project p = Project.builder()
                .name(dto.name().trim())
                .description(dto.description())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .active(dto.active() == null ? true : dto.active())
                .projectManager(pm)

                // NEW
                .portfolioName(dto.portfolioName())
                .programName(dto.programName())
                .subProgramName(dto.subProgramName())
                .objective(dto.objective())
                .calendarName(dto.calendarName())
                .baselineStartDate(dto.baselineStartDate())
                .baselineEndDate(dto.baselineEndDate())
                .progress(dto.progress() == null ? 0 : dto.progress())
                .build();

        Project saved = projectRepository.save(p);
        return Optional.of(toDTO(saved));
    }

    @Override
    public List<ProjectDTO> getAll() {
        // If you want only active projects by default, swap to findByActiveTrue()
        return projectRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    public Optional<ProjectDTO> getById(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return projectRepository.findById(id).map(this::toDTO);
    }

    @Override
    public List<ProjectDTO> getByManager(Long managerId) {
        if (managerId == null || managerId <= 0) return List.of();
        return projectRepository.findByProjectManagerId(managerId).stream().map(this::toDTO).toList();
    }

    @Override
    public Optional<ProjectDTO> update(Long id, ProjectCreateUpdateDTO dto) {
        if (id == null || id <= 0 || dto == null) return Optional.empty();

        Project p = projectRepository.findById(id).orElse(null);
        if (p == null) return Optional.empty();

        if (dto.name() != null && !dto.name().isBlank()) p.setName(dto.name().trim());
        if (dto.description() != null) p.setDescription(dto.description());
        if (dto.startDate() != null) p.setStartDate(dto.startDate());
        p.setEndDate(dto.endDate());

        if (dto.active() != null) p.setActive(dto.active());

        if (dto.projectManagerId() != null) {
            User pm = userRepository.findById(dto.projectManagerId()).orElse(null);
            if (pm == null) return Optional.empty();
            p.setProjectManager(pm);
        }

        // NEW fields updates (null = keep existing)
        if (dto.portfolioName() != null) p.setPortfolioName(dto.portfolioName());
        if (dto.programName() != null) p.setProgramName(dto.programName());
        if (dto.subProgramName() != null) p.setSubProgramName(dto.subProgramName());
        if (dto.objective() != null) p.setObjective(dto.objective());
        if (dto.calendarName() != null) p.setCalendarName(dto.calendarName());
        if (dto.baselineStartDate() != null) p.setBaselineStartDate(dto.baselineStartDate());
        if (dto.baselineEndDate() != null) p.setBaselineEndDate(dto.baselineEndDate());
        if (dto.progress() != null) p.setProgress(dto.progress());

        Project saved = projectRepository.save(p);
        return Optional.of(toDTO(saved));
    }

    @Override
    public boolean deactivate(Long id) {
        if (id == null || id <= 0) return false;

        Project p = projectRepository.findById(id).orElse(null);
        if (p == null) return false;

        p.setActive(false);
        projectRepository.save(p);
        return true;
    }

    private ProjectDTO toDTO(Project p) {
        return new ProjectDTO(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getStartDate(),
                p.getEndDate(),
                p.isActive(),
                p.getProjectManager() != null ? p.getProjectManager().getId() : null,

                // NEW
                p.getPortfolioName(),
                p.getProgramName(),
                p.getSubProgramName(),
                p.getObjective(),
                p.getCalendarName(),
                p.getBaselineStartDate(),
                p.getBaselineEndDate(),
                p.getProgress()
        );
    }
}