package org.pfe.ppm_project.repositories;

import org.pfe.ppm_project.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Toutes les notifications d'un destinataire, newest-first.
     * Utilisé pour la page Notifications Center.
     */
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    /**
     * Notifications non lues d'un destinataire — pour le compteur du badge.
     */
    List<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(Long recipientId);

    /**
     * Compte des notifications non lues — appel leger pour le header.
     */
    long countByRecipientIdAndReadFalse(Long recipientId);

    /**
     * Marque toutes les notifications non lues d'un utilisateur comme lues.
     * Bulk update pour eviter N+1 lors du "tout marquer comme lu".
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientId = :recipientId AND n.read = false")
    int markAllAsRead(@Param("recipientId") Long recipientId);
}