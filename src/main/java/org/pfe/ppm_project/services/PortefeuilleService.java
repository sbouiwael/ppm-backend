package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.PortefeuilleCreateUpdateDTO;
import org.pfe.ppm_project.dto.PortefeuilleDTO;
import org.pfe.ppm_project.dto.ProjectDTO;
import org.pfe.ppm_project.entities.Portefeuille;
import org.pfe.ppm_project.entities.Project;
import org.pfe.ppm_project.enums.AuditAction;
import org.pfe.ppm_project.repositories.PortefeuilleRepository;
import org.pfe.ppm_project.repositories.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service metier pour la gestion des portefeuilles de projets.
 * Gere le CRUD des portefeuilles ainsi que l'ajout et le retrait
 * de projets au sein d'un portefeuille.
 */
@Service
@RequiredArgsConstructor
public class PortefeuilleService implements IPortefeuilleService {

    private final PortefeuilleRepository portefeuilleRepository;
    private final ProjectRepository projectRepository;
    private final IAuditLogService auditLogService;

    // Cree un nouveau portefeuille a partir du DTO
    @Override
    public Optional<PortefeuilleDTO> create(PortefeuilleCreateUpdateDTO dto) {
        if (dto == null || dto.nom() == null || dto.nom().isBlank()) {
            return Optional.empty();
        }

        Portefeuille p = Portefeuille.builder()
                .nom(dto.nom().trim())
                .description(dto.description())
                .build();

        Portefeuille saved = portefeuilleRepository.save(p);
        auditLogService.log(
                AuditAction.CREATE, "PORTFOLIO", saved.getId(), saved.getNom(),
                "Portfolio created", null, null
        );
        return Optional.of(toDTO(saved));
    }

    // Retourne la liste de tous les portefeuilles avec leurs projets
    @Override
    public List<PortefeuilleDTO> getAll() {
        return portefeuilleRepository.findAll().stream().map(this::toDTO).toList();
    }

    // Retourne un portefeuille par son identifiant
    @Override
    public Optional<PortefeuilleDTO> getById(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return portefeuilleRepository.findById(id).map(this::toDTO);
    }

    // Met a jour un portefeuille existant (nom et description)
    @Override
    public Optional<PortefeuilleDTO> update(Long id, PortefeuilleCreateUpdateDTO dto) {
        if (id == null || id <= 0 || dto == null) return Optional.empty();

        Portefeuille p = portefeuilleRepository.findById(id).orElse(null);
        if (p == null) return Optional.empty();

        if (dto.nom() != null && !dto.nom().isBlank()) p.setNom(dto.nom().trim());
        if (dto.description() != null) p.setDescription(dto.description());

        Portefeuille saved = portefeuilleRepository.save(p);
        auditLogService.log(
                AuditAction.UPDATE, "PORTFOLIO", saved.getId(), saved.getNom(),
                "Portfolio updated", null, null
        );
        return Optional.of(toDTO(saved));
    }

    // Supprime un portefeuille apres avoir detache tous ses projets
    @Override
    @Transactional
    public boolean delete(Long id) {
        if (id == null || id <= 0) return false;

        Portefeuille p = portefeuilleRepository.findById(id).orElse(null);
        if (p == null) return false;

        String nom = p.getNom();
        // Bulk detach — single UPDATE instead of N individual saves
        projectRepository.detachAllFromPortefeuille(id);
        portefeuilleRepository.delete(p);
        auditLogService.log(
                AuditAction.DELETE, "PORTFOLIO", id, nom,
                "Portfolio deleted", null, null
        );
        return true;
    }

    // Ajoute un projet a un portefeuille (uniquement si le projet n'est pas deja affecte)
    @Override
    @Transactional
    public Optional<PortefeuilleDTO> addProject(Long portefeuilleId, Long projectId) {
        if (portefeuilleId == null || projectId == null) return Optional.empty();

        Portefeuille pf = portefeuilleRepository.findById(portefeuilleId).orElse(null);
        if (pf == null) return Optional.empty();

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return Optional.empty();

        // Empeche l'affectation si le projet appartient deja a un portefeuille
        if (project.getPortefeuille() != null) return Optional.empty();

        project.setPortefeuille(pf);
        projectRepository.save(project);

        // Recharge pour obtenir la liste de projets mise a jour
        Portefeuille updated = portefeuilleRepository.findById(portefeuilleId).orElse(pf);
        return Optional.of(toDTO(updated));
    }

    // Retire un projet d'un portefeuille (uniquement s'il appartient bien a ce portefeuille)
    @Override
    @Transactional
    public Optional<PortefeuilleDTO> removeProject(Long portefeuilleId, Long projectId) {
        if (portefeuilleId == null || projectId == null) return Optional.empty();

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return Optional.empty();

        // Verifie que le projet appartient bien a ce portefeuille
        if (project.getPortefeuille() == null || !project.getPortefeuille().getId().equals(portefeuilleId)) {
            return Optional.empty();
        }

        project.setPortefeuille(null);
        projectRepository.save(project);

        Portefeuille updated = portefeuilleRepository.findById(portefeuilleId).orElse(null);
        if (updated == null) return Optional.empty();
        return Optional.of(toDTO(updated));
    }

    // Retourne les projets non affectes a aucun portefeuille
    @Override
    public List<ProjectDTO> getUnassignedProjects() {
        return projectRepository.findByPortefeuilleIsNull().stream()
                .map(this::toProjectDTO)
                .toList();
    }

    // Convertit un Portefeuille en DTO avec sa liste de projets
    private PortefeuilleDTO toDTO(Portefeuille p) {
        List<ProjectDTO> projectDTOs = p.getProjects() != null
                ? p.getProjects().stream().map(this::toProjectDTO).toList()
                : List.of();

        return new PortefeuilleDTO(p.getId(), p.getNom(), p.getDescription(), projectDTOs);
    }

    // Convertit un Project en ProjectDTO
    private ProjectDTO toProjectDTO(Project p) {
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
