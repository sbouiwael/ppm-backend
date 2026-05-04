package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pfe.ppm_project.dto.AuditLogDTO;
import org.pfe.ppm_project.entities.AuditLog;
import org.pfe.ppm_project.enums.AuditAction;
import org.pfe.ppm_project.repositories.AuditLogRepository;
import org.pfe.ppm_project.security.AuditContextUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation du service d'audit PPM.
 *
 * RESPONSABILITE UNIQUE : persister les entrees d'audit sans modifier l'etat metier.
 * Ce service ne doit JAMAIS lancer d'exception qui interromprait le flux metier appelant.
 *
 * L'acteur est extrait automatiquement depuis le SecurityContextHolder via AuditContextUtil.
 * En cas d'absence de contexte (tests unitaires, taches schedulees), actorEmail = "system".
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuditLogService implements IAuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Enregistre une action dans le journal d'audit.
     *
     * DESIGN : l'acteur est extrait ici pour ne pas polluer les services metier avec
     * des appels a SecurityContextHolder. Les services passent uniquement les donnees
     * metier (qui, quoi, sur quelle entite, dans quel projet).
     */
    // REQUIRES_NEW : l'audit tourne dans sa propre transaction independante.
    // Si elle echoue (ex: charset MySQL, schema manquant), elle se rollback seule
    // sans corrompre la session Hibernate du service appelant.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            AuditAction action,
            String entityType,
            Long entityId,
            String entityName,
            String details,
            Long projectId,
            String projectName
    ) {
        try {
            AuditLog entry = AuditLog.builder()
                    .actorId(AuditContextUtil.getCurrentUserId())
                    .actorEmail(AuditContextUtil.getCurrentUserEmail())
                    .actorRole(AuditContextUtil.getCurrentUserRole())
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityName(entityName != null ? entityName : "")
                    .details(details)
                    .projectId(projectId)
                    .projectName(projectName)
                    .build();

            auditLogRepository.save(entry);
        } catch (Exception ex) {
            // L'audit ne doit jamais faire echouer une operation metier legitime.
            // Si la sauvegarde echoue (ex: schema pas encore migre), on log en warn.
            log.warn("[AUDIT] Failed to save audit entry for action={} entity={}:{} — {}",
                    action, entityType, entityId, ex.getMessage());
        }
    }

    /**
     * Recherche paginee avec filtres optionnels.
     * Retourne les entrees les plus recentes en premier.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDTO> search(
            String entityType,
            Long actorId,
            AuditAction action,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        // Clamp page size pour eviter des requetes excessives
        int clampedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                clampedSize,
                Sort.by(Sort.Direction.DESC, "timestamp")
        );
        return auditLogRepository.search(entityType, actorId, action, from, to, pageable)
                .map(this::toDTO);
    }

    /** Historique complet d'une entite (trie par timestamp desc). */
    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDTO> getByEntity(String entityType, Long entityId) {
        return auditLogRepository
                .findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId)
                .stream().map(this::toDTO).toList();
    }

    /** Historique de toutes les actions dans un projet. */
    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDTO> getByProject(Long projectId) {
        return auditLogRepository
                .findByProjectIdOrderByTimestampDesc(projectId)
                .stream().map(this::toDTO).toList();
    }

    /** Historique des actions d'un acteur specifique. */
    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDTO> getByActor(Long actorId) {
        return auditLogRepository
                .findByActorIdOrderByTimestampDesc(actorId)
                .stream().map(this::toDTO).toList();
    }

    // Conversion entite -> DTO
    private AuditLogDTO toDTO(AuditLog a) {
        return new AuditLogDTO(
                a.getId(),
                a.getActorId(),
                a.getActorEmail(),
                a.getActorRole(),
                a.getAction(),
                a.getEntityType(),
                a.getEntityId(),
                a.getEntityName(),
                a.getDetails(),
                a.getProjectId(),
                a.getProjectName(),
                a.getTimestamp()
        );
    }
}
