package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.ProjectCreateUpdateDTO;
import org.pfe.ppm_project.dto.ProjectDTO;
import org.pfe.ppm_project.entities.Project;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.entities.User;
import org.pfe.ppm_project.enums.AuditAction;
import org.pfe.ppm_project.exception.ResourceNotFoundException;
import org.pfe.ppm_project.repositories.ProjectRepository;
import org.pfe.ppm_project.repositories.TaskAssignmentRepository;
import org.pfe.ppm_project.repositories.TaskDependencyRepository;
import org.pfe.ppm_project.repositories.TaskRepository;
import org.pfe.ppm_project.repositories.UserRepository;
import org.pfe.ppm_project.repositories.WorkCalendarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Service metier pour la gestion des projets.
 * Gere la creation, la lecture, la mise a jour et la desactivation des projets.
 * Cree egalement les dossiers physiques du projet via FileStorageService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService implements IProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final IFileStorageService fileStorageService;
    private final WorkCalendarRepository calendarRepository;
    private final IAuditLogService auditLogService;
    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    // Cree un nouveau projet a partir du DTO. Verifie que le chef de projet existe.
    @Override
    public Optional<ProjectDTO> create(ProjectCreateUpdateDTO dto) {
        // Validation des champs obligatoires
        if (dto == null || dto.name() == null || dto.name().isBlank()
                || dto.startDate() == null || dto.projectManagerId() == null) {
            return Optional.empty();
        }

        // Recherche du chef de projet
        User pm = userRepository.findById(dto.projectManagerId()).orElse(null);
        if (pm == null) return Optional.empty();

        // Construction de l'entite projet
        Project p = Project.builder()
                .name(dto.name().trim())
                .description(dto.description())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .active(dto.active() == null ? true : dto.active())
                .projectManager(pm)

                // Champs portefeuille et programme
                .portfolioName(dto.portfolioName())
                .programName(dto.programName())
                .subProgramName(dto.subProgramName())
                .objective(dto.objective())
                .calendarName(dto.calendarName())
                .baselineStartDate(dto.baselineStartDate())
                .baselineEndDate(dto.baselineEndDate())
                .progress(dto.progress() == null ? 0 : dto.progress())
                .calendar(dto.calendarId() != null ? calendarRepository.findById(dto.calendarId()).orElse(null) : null)
                .build();

        Project saved = projectRepository.save(p);

        // Creation des dossiers physiques (fonctions, P.V, contrats)
        try {
            fileStorageService.createProjectFolders(saved.getName());
        } catch (Exception e) {
            log.warn("Failed to create project folders for '{}': {}", saved.getName(), e.getMessage());
        }

        // Audit : enregistre la creation du projet avec le nom du chef de projet
        auditLogService.log(
                AuditAction.CREATE, "PROJECT", saved.getId(), saved.getName(),
                "Project created by " + (pm.getFirstName() + " " + pm.getLastName()).trim(),
                saved.getId(), saved.getName()
        );

        return Optional.of(toDTO(saved));
    }

    // Retourne la liste de tous les projets (actifs et inactifs)
    @Override
    @Transactional(readOnly = true)
    public List<ProjectDTO> getAll() {
        return projectRepository.findAll().stream().map(this::toDTO).toList();
    }

    // Retourne un projet par son identifiant
    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectDTO> getById(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return projectRepository.findById(id).map(this::toDTO);
    }

    // Retourne les projets geres par un chef de projet donne
    @Override
    @Transactional(readOnly = true)
    public List<ProjectDTO> getByManager(Long managerId) {
        if (managerId == null || managerId <= 0) return List.of();
        return projectRepository.findByProjectManagerId(managerId).stream().map(this::toDTO).toList();
    }

    // Met a jour un projet existant. Les champs null dans le DTO conservent la valeur existante.
    @Override
    public Optional<ProjectDTO> update(Long id, ProjectCreateUpdateDTO dto) {
        if (id == null || id <= 0 || dto == null) return Optional.empty();

        Project p = projectRepository.findById(id).orElse(null);
        if (p == null) return Optional.empty();

        // Mise a jour conditionnelle des champs principaux
        if (dto.name() != null && !dto.name().isBlank()) p.setName(dto.name().trim());
        if (dto.description() != null) p.setDescription(dto.description());
        if (dto.startDate() != null) p.setStartDate(dto.startDate());
        p.setEndDate(dto.endDate());

        if (dto.active() != null) p.setActive(dto.active());

        // Changement de chef de projet (verification d'existence)
        if (dto.projectManagerId() != null) {
            User pm = userRepository.findById(dto.projectManagerId()).orElse(null);
            if (pm == null) return Optional.empty();
            p.setProjectManager(pm);
        }

        // Mise a jour des champs portefeuille/programme (null = conserver l'existant)
        if (dto.portfolioName() != null) p.setPortfolioName(dto.portfolioName());
        if (dto.programName() != null) p.setProgramName(dto.programName());
        if (dto.subProgramName() != null) p.setSubProgramName(dto.subProgramName());
        if (dto.objective() != null) p.setObjective(dto.objective());
        if (dto.calendarName() != null) p.setCalendarName(dto.calendarName());
        if (dto.baselineStartDate() != null) p.setBaselineStartDate(dto.baselineStartDate());
        if (dto.baselineEndDate() != null) p.setBaselineEndDate(dto.baselineEndDate());
        if (dto.progress() != null) p.setProgress(dto.progress());

        // Association au calendrier de travail
        if (dto.calendarId() != null) {
            p.setCalendar(calendarRepository.findById(dto.calendarId()).orElse(null));
        }

        Project saved = projectRepository.save(p);

        // Audit : enregistre la modification du projet
        auditLogService.log(
                AuditAction.UPDATE, "PROJECT", saved.getId(), saved.getName(),
                "Project updated",
                saved.getId(), saved.getName()
        );

        return Optional.of(toDTO(saved));
    }

    // Active ou desactive un projet sans modifier ses autres donnees
    @Override
    public void setProjectActive(Long id, boolean active) {
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        p.setActive(active);
        projectRepository.save(p);
        auditLogService.log(
                AuditAction.UPDATE, "PROJECT", p.getId(), p.getName(),
                active ? "Project activated" : "Project deactivated",
                p.getId(), p.getName()
        );
    }

    // Suppression physique d'un projet avec cascade complete : dependances, affectations, taches
    @Override
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));

        String projectName = project.getName();

        // Recupere toutes les taches du projet
        List<Task> tasks = taskRepository.findByProjectId(id);
        List<Long> taskIds = tasks.stream().map(Task::getId).toList();

        if (!taskIds.isEmpty()) {
            // Single DELETE for all dependencies involving these tasks
            taskDependencyRepository.deleteByTaskIds(taskIds);
            // Single DELETE for all assignments across all tasks
            taskAssignmentRepository.deleteByTaskIdIn(taskIds);
            // Supprime les taches (les sous-taches sont aussi dans la liste via findByProjectId)
            taskRepository.deleteAll(tasks);
        }

        projectRepository.delete(project);

        auditLogService.log(
                AuditAction.DELETE, "PROJECT", id, projectName,
                "Project permanently deleted",
                id, projectName
        );
    }

    // Verifie si un utilisateur est le chef de projet designe pour ce projet
    @Override
    @Transactional(readOnly = true)
    public boolean isProjectManager(Long projectId, Long userId) {
        if (projectId == null || userId == null) return false;
        return projectRepository.findById(projectId)
                .map(p -> p.getProjectManager() != null && p.getProjectManager().getId().equals(userId))
                .orElse(false);
    }

    // Desactive un projet (suppression logique)
    @Override
    public boolean deactivate(Long id) {
        if (id == null || id <= 0) return false;

        Project p = projectRepository.findById(id).orElse(null);
        if (p == null) return false;

        p.setActive(false);
        projectRepository.save(p);

        // Audit : enregistre la desactivation du projet
        auditLogService.log(
                AuditAction.DELETE, "PROJECT", p.getId(), p.getName(),
                "Project deactivated (soft delete)",
                p.getId(), p.getName()
        );

        return true;
    }

    // Convertit une entite Project en ProjectDTO
    private ProjectDTO toDTO(Project p) {
        String managerName = null;
        if (p.getProjectManager() != null) {
            String first = p.getProjectManager().getFirstName();
            String last  = p.getProjectManager().getLastName();
            managerName  = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
            if (managerName.isEmpty()) managerName = null;
        }
        return new ProjectDTO(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getStartDate(),
                p.getEndDate(),
                p.isActive(),
                p.getProjectManager() != null ? p.getProjectManager().getId() : null,
                managerName,

                p.getPortfolioName(),
                p.getProgramName(),
                p.getSubProgramName(),
                p.getObjective(),
                p.getCalendarName(),
                p.getBaselineStartDate(),
                p.getBaselineEndDate(),
                p.getProgress(),
                p.getPortefeuille() != null ? p.getPortefeuille().getId() : null,
                p.getCalendar() != null ? p.getCalendar().getId() : null
        );
    }
}
