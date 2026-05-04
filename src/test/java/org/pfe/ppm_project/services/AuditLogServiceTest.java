package org.pfe.ppm_project.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pfe.ppm_project.entities.AuditLog;
import org.pfe.ppm_project.enums.AuditAction;
import org.pfe.ppm_project.repositories.AuditLogRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AuditLogService.
 * Verifie la creation d'entrees d'audit, la resilience aux erreurs,
 * et les methodes de recherche.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService — journal d'audit")
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService service;

    @BeforeEach
    void setUp() {
        service = new AuditLogService(auditLogRepository);
    }

    // ── log() ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("log: sauvegarde une entree d'audit avec les bonnes metadonnees")
    void log_savesEntryWithCorrectFields() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.log(AuditAction.CREATE, "PROJECT", 42L, "BIAT Core Banking",
                "Project created", 42L, "BIAT Core Banking");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(saved.getEntityType()).isEqualTo("PROJECT");
        assertThat(saved.getEntityId()).isEqualTo(42L);
        assertThat(saved.getEntityName()).isEqualTo("BIAT Core Banking");
        assertThat(saved.getDetails()).isEqualTo("Project created");
        assertThat(saved.getProjectId()).isEqualTo(42L);
        // actorEmail defaults to "system" when no SecurityContext
        assertThat(saved.getActorEmail()).isEqualTo("system");
    }

    @Test
    @DisplayName("log: ne leve pas d'exception si le repository echoue (resilience metier)")
    void log_doesNotThrow_whenRepositoryFails() {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        // Doit silencieusement absorber l'erreur (log warn, ne propage pas)
        assertThatNoException().isThrownBy(() ->
                service.log(AuditAction.UPDATE, "TASK", 1L, "Tache A", "Updated", null, null)
        );
    }

    @Test
    @DisplayName("log: entity name null est normalise en chaine vide")
    void log_normalizesNullEntityName() {
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.log(AuditAction.DELETE, "USER", 5L, null, "Deleted", null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityName()).isEqualTo("");
    }

    // ── search() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search: retourne les resultats pagines depuis le repository")
    void search_returnsMappedPage() {
        AuditLog entry = AuditLog.builder()
                .id(1L).action(AuditAction.CREATE).entityType("TASK").entityId(10L)
                .entityName("Sprint 1").actorEmail("admin@biat.com").actorRole("ADMIN")
                .timestamp(LocalDateTime.now()).build();

        when(auditLogRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry)));

        var result = service.search("TASK", null, AuditAction.CREATE, null, null, 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).entityType()).isEqualTo("TASK");
        assertThat(result.getContent().get(0).action()).isEqualTo(AuditAction.CREATE);
    }

    @Test
    @DisplayName("search: clamp la taille de page entre 1 et 100")
    void search_clampsPageSize() {
        when(auditLogRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Taille 0 est clampee a 1
        service.search(null, null, null, null, null, 0, 0);
        // Taille 9999 est clampee a 100
        service.search(null, null, null, null, null, 0, 9999);

        verify(auditLogRepository, times(2)).search(any(), any(), any(), any(), any(),
                argThat(p -> p.getPageSize() >= 1 && p.getPageSize() <= 100));
    }

    // ── getByEntity() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByEntity: delègue au repository avec entityType et entityId")
    void getByEntity_delegatesToRepository() {
        when(auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("PROJECT", 42L))
                .thenReturn(List.of());

        var result = service.getByEntity("PROJECT", 42L);

        assertThat(result).isEmpty();
        verify(auditLogRepository).findByEntityTypeAndEntityIdOrderByTimestampDesc("PROJECT", 42L);
    }
}
