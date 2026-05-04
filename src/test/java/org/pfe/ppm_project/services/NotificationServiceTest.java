package org.pfe.ppm_project.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pfe.ppm_project.entities.Notification;
import org.pfe.ppm_project.enums.NotificationType;
import org.pfe.ppm_project.exception.InvalidOperationException;
import org.pfe.ppm_project.exception.ResourceNotFoundException;
import org.pfe.ppm_project.repositories.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour NotificationService.
 * Verifie l'anti-spam, la visibilite par destinataire, et les mutations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService — centre de notifications")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    // SseEmitterRegistry has no external dependencies — use a real instance
    private final SseEmitterRegistry sseEmitterRegistry = new SseEmitterRegistry();

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, sseEmitterRegistry);
    }

    // ── createNotification ───────────────────────────────────────────────────

    @Test
    @DisplayName("create: cree une notification si aucun doublon non lu n'existe")
    void create_savesNotification_whenNoDuplicate() {
        when(notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(1L))
                .thenReturn(List.of()); // aucune notification non lue existante

        // save() doit retourner une entite avec ID (pour le push SSE)
        when(notificationRepository.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            return Notification.builder()
                    .id(1L).recipientId(n.getRecipientId()).type(n.getType())
                    .title(n.getTitle()).message(n.getMessage())
                    .entityType(n.getEntityType()).entityId(n.getEntityId())
                    .read(false).build();
        });
        when(notificationRepository.countByRecipientIdAndReadFalse(1L)).thenReturn(1L);

        service.createNotification(1L, NotificationType.TASK_ASSIGNED,
                "New task", "Task assigned", "TASK", 10L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
        assertThat(captor.getValue().getRecipientId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("create: ne duplique pas si meme notification non lue existe deja (anti-spam)")
    void create_skipsDuplicate_whenSameUnreadExists() {
        Notification existing = Notification.builder()
                .id(99L).recipientId(1L).type(NotificationType.TASK_ASSIGNED)
                .entityType("TASK").entityId(10L).read(false)
                .createdAt(LocalDateTime.now()).build();

        when(notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(existing));

        service.createNotification(1L, NotificationType.TASK_ASSIGNED,
                "New task", "Task assigned", "TASK", 10L);

        // Ne doit pas sauvegarder (doublon detecte)
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("create: ne fait rien si recipientId est null")
    void create_doesNothing_whenRecipientNull() {
        service.createNotification(null, NotificationType.TASK_ASSIGNED,
                "Title", "Msg", "TASK", 1L);
        verify(notificationRepository, never()).findByRecipientIdAndReadFalseOrderByCreatedAtDesc(any());
        verify(notificationRepository, never()).save(any());
    }

    // ── markAsRead ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("markAsRead: met read=true si la notification appartient a l'utilisateur")
    void markAsRead_setsReadTrue_whenOwner() {
        Notification notif = Notification.builder()
                .id(1L).recipientId(5L).read(false)
                .type(NotificationType.TASK_ASSIGNED)
                .createdAt(LocalDateTime.now()).title("T").build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notif));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markAsRead(1L, 5L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().isRead()).isTrue();
    }

    @Test
    @DisplayName("markAsRead: leve InvalidOperationException si l'utilisateur n'est pas le destinataire")
    void markAsRead_throwsException_whenNotOwner() {
        Notification notif = Notification.builder()
                .id(1L).recipientId(5L).read(false)
                .type(NotificationType.TASK_ASSIGNED)
                .createdAt(LocalDateTime.now()).title("T").build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notif));

        // userId=99 n'est pas le destinataire (recipientId=5)
        assertThatThrownBy(() -> service.markAsRead(1L, 99L))
                .isInstanceOf(InvalidOperationException.class);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAsRead: leve ResourceNotFoundException si notification inconnue")
    void markAsRead_throwsNotFound_whenMissing() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── markAllAsRead ────────────────────────────────────────────────────────

    @Test
    @DisplayName("markAllAsRead: appelle le bulk update du repository")
    void markAllAsRead_callsBulkUpdate() {
        when(notificationRepository.markAllAsRead(3L)).thenReturn(5);

        service.markAllAsRead(3L);

        verify(notificationRepository, times(1)).markAllAsRead(3L);
    }

    // ── countUnread ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("countUnread: retourne le compteur du repository")
    void countUnread_returnsRepositoryCount() {
        when(notificationRepository.countByRecipientIdAndReadFalse(7L)).thenReturn(3L);

        long count = service.countUnread(7L);

        assertThat(count).isEqualTo(3L);
    }
}
