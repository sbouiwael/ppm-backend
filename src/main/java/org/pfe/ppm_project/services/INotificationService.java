package org.pfe.ppm_project.services;

import org.pfe.ppm_project.dto.NotificationDTO;
import org.pfe.ppm_project.enums.NotificationType;

import java.util.List;

/**
 * Contrat du service de gestion des notifications PPM.
 *
 * PRINCIPE DE BASE : chaque utilisateur ne voit que ses propres notifications.
 * Le filtrage par recipientId est effectue systematiquement dans chaque methode.
 */
public interface INotificationService {

    /**
     * Cree une notification pour un utilisateur.
     * Ne cree pas la notification si une notification identique (meme type + entite)
     * non lue existe deja, pour eviter le spam.
     */
    void createNotification(
            Long recipientId,
            NotificationType type,
            String title,
            String message,
            String entityType,
            Long entityId
    );

    /** Toutes les notifications d'un utilisateur, newest-first. */
    List<NotificationDTO> getMyNotifications(Long userId);

    /** Notifications non lues d'un utilisateur (pour la page filtre "non lu"). */
    List<NotificationDTO> getUnreadNotifications(Long userId);

    /** Nombre de notifications non lues — pour le badge dans le header. */
    long countUnread(Long userId);

    /** Marque une notification specifique comme lue (verifie que c'est bien le destinataire). */
    void markAsRead(Long notificationId, Long userId);

    /** Marque toutes les notifications de l'utilisateur comme lues. */
    void markAllAsRead(Long userId);

    /** Supprime une notification (seul le destinataire peut la supprimer). */
    void deleteNotification(Long notificationId, Long userId);
}