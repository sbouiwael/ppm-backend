package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pfe.ppm_project.dto.NotificationDTO;
import org.pfe.ppm_project.entities.Notification;
import org.pfe.ppm_project.enums.NotificationType;
import org.pfe.ppm_project.exception.ResourceNotFoundException;
import org.pfe.ppm_project.repositories.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Implementation du service de notifications PPM.
 *
 * SECURITE : chaque methode qui modifie ou expose des donnees verifie que
 * l'utilisateur courant (userId) est bien le destinataire (recipientId).
 * Cela empeche un utilisateur de lire ou modifier les notifications d'un autre.
 *
 * ANTI-SPAM : createNotification() verifie l'existence d'une notification identique
 * non lue avant de creer une nouvelle entree. Cela evite d'inonder l'utilisateur
 * si le meme evenement se produit en boucle (ex: multi-save sur un formulaire).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterRegistry     sseRegistry;

    /**
     * Cree une notification uniquement si aucune notification identique non lue
     * n'existe deja pour ce destinataire et cette entite.
     *
     * LOGIQUE ANTI-SPAM : compare recipientId + type + entityType + entityId.
     * Ex: si l'utilisateur est deja notifie de l'assignation a la tache 42,
     * une seconde notification pour la meme tache ne sera pas creee.
     */
    @Override
    public void createNotification(
            Long recipientId,
            NotificationType type,
            String title,
            String message,
            String entityType,
            Long entityId
    ) {
        if (recipientId == null) return;

        // Controle anti-spam : ne pas creer une notification deja presente et non lue
        boolean alreadyExists = notificationRepository
                .findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId)
                .stream()
                .anyMatch(n -> n.getType() == type
                        && objectEquals(n.getEntityType(), entityType)
                        && objectEquals(n.getEntityId(), entityId));

        if (alreadyExists) {
            log.debug("[NOTIFICATION] Skipped duplicate: recipientId={} type={} entity={}:{}",
                    recipientId, type, entityType, entityId);
            return;
        }

        Notification notif = Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .title(title)
                .message(message)
                .entityType(entityType)
                .entityId(entityId)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notif);

        // Pousse l'evenement SSE aux connexions actives de ce destinataire.
        // Payload minimal : le frontend met a jour son badge en temps reel.
        // Le push SSE est optionnel : si le client n'est pas connecte (emitter expire ou absent),
        // l'exception est silencieusement ignoree — la notification est deja persistee en base.
        long newCount = notificationRepository.countByRecipientIdAndReadFalse(recipientId);
        try {
            sseRegistry.push(recipientId, "notification", Map.of(
                    "id",    saved.getId(),
                    "type",  saved.getType().name(),
                    "title", saved.getTitle(),
                    "count", newCount
            ));
        } catch (Exception e) {
            log.debug("[NOTIFICATION] SSE push failed for userId={} — client likely not connected: {}",
                    recipientId, e.getMessage());
        }
    }

    /** Toutes les notifications du destinataire, newest-first. */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getMyNotifications(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDTO).toList();
    }

    /** Notifications non lues uniquement. */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(Long userId) {
        return notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::toDTO).toList();
    }

    /** Compteur de notifications non lues (leger, pour le badge). */
    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    /**
     * Marque une notification comme lue.
     * Verifie que la notification appartient bien a userId pour empecher
     * qu'un utilisateur marque les notifications d'un autre.
     */
    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notif.getRecipientId().equals(userId)) {
            throw new org.pfe.ppm_project.exception.InvalidOperationException(
                    "Not allowed to modify this notification");
        }

        notif.setRead(true);
        notificationRepository.save(notif);
    }

    /**
     * Marque toutes les notifications non lues de l'utilisateur comme lues.
     * Utilise un bulk update SQL pour eviter N+1.
     */
    @Override
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    /**
     * Supprime une notification — uniquement accessible a son destinataire.
     */
    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notif.getRecipientId().equals(userId)) {
            throw new org.pfe.ppm_project.exception.InvalidOperationException(
                    "Not allowed to delete this notification");
        }

        notificationRepository.delete(notif);
    }

    // Conversion entite -> DTO
    private NotificationDTO toDTO(Notification n) {
        return new NotificationDTO(
                n.getId(),
                n.getRecipientId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.isRead(),
                n.getEntityType(),
                n.getEntityId(),
                n.getCreatedAt()
        );
    }

    private boolean objectEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}