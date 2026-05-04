package org.pfe.ppm_project.repositories;

import org.pfe.ppm_project.entities.AuditLog;
import org.pfe.ppm_project.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Recherche paginee multi-criteres.
     * Chaque filtre est optionnel : null = pas de filtre sur ce critere.
     * ORDER BY timestamp DESC = les evenements les plus recents en premier.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:entityType IS NULL OR a.entityType = :entityType)
              AND (:actorId    IS NULL OR a.actorId    = :actorId)
              AND (:action     IS NULL OR a.action     = :action)
              AND (:from       IS NULL OR a.timestamp  >= :from)
              AND (:to         IS NULL OR a.timestamp  <= :to)
            ORDER BY a.timestamp DESC
            """)
    Page<AuditLog> search(
            @Param("entityType") String entityType,
            @Param("actorId")    Long actorId,
            @Param("action")     AuditAction action,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            Pageable pageable
    );

    /** Historique d'une entite specifique (ex: toutes les modifs du projet 42). */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
            String entityType, Long entityId);

    /** Toutes les actions effectuees dans le contexte d'un projet (vue PM). */
    List<AuditLog> findByProjectIdOrderByTimestampDesc(Long projectId);

    /** Toutes les actions effectuees par un acteur donne (vue ADMIN). */
    List<AuditLog> findByActorIdOrderByTimestampDesc(Long actorId);
}