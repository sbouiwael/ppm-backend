package org.pfe.ppm_project.controller;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.NotificationDTO;
import org.pfe.ppm_project.services.INotificationService;
import org.pfe.ppm_project.services.SseEmitterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * Controller REST pour le centre de notifications PPM.
 *
 * SECURITE : toutes les operations utilisent l'userId extrait du JWT (SecurityContext),
 * jamais un userId fourni dans l'URL. Cela garantit qu'un utilisateur ne peut
 * acceder qu'a ses propres notifications (isolation stricte).
 *
 * L'userId est stocke dans les details du token par JwtAuthenticationFilter
 * (authToken.setDetails(userId)) — meme pattern que TaskAssignmentController.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;
    private final SseEmitterRegistry   sseRegistry;

    // ── SSE Stream ────────────────────────────────────────────────────────

    /**
     * Ouvre une connexion SSE persistante pour recevoir les notifications en temps reel.
     *
     * TOKEN : EventSource ne supporte pas les headers custom, donc le JWT est accepte
     * via le query param ?token= (gere par JwtAuthenticationFilter.extractToken()).
     *
     * Le navigateur reconnecte automatiquement apres un timeout ou une perte de connexion
     * (comportement natif d'EventSource, pas besoin de logique cote serveur).
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications() {
        return sseRegistry.register(currentUserId());
    }

    // ── Lecture ────────────────────────────────────────────────────────────

    /**
     * Toutes mes notifications, newest-first.
     * Utilisé pour la page Notifications Center.
     */
    @GetMapping("/me")
    public ResponseEntity<List<NotificationDTO>> getMyNotifications() {
        return ResponseEntity.ok(notificationService.getMyNotifications(currentUserId()));
    }

    /**
     * Mes notifications non lues uniquement.
     * Utile pour un filtre "non lu" dans l'UI.
     */
    @GetMapping("/me/unread")
    public ResponseEntity<List<NotificationDTO>> getUnread() {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(currentUserId()));
    }

    /**
     * Compteur de notifications non lues — pour le badge dans le header.
     * Endpoint leger : retourne juste un entier.
     */
    @GetMapping("/me/unread-count")
    public ResponseEntity<Map<String, Long>> countUnread() {
        long count = notificationService.countUnread(currentUserId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ── Mutations ──────────────────────────────────────────────────────────

    /**
     * Marque une notification specifique comme lue.
     * Verifie que la notification appartient bien a l'utilisateur courant.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id, currentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Marque toutes mes notifications comme lues en une seule operation.
     * Bulk update SQL — efficace meme avec un grand nombre de notifications.
     */
    @PutMapping("/me/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead(currentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Supprime une notification.
     * Seul le destinataire peut supprimer sa propre notification.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id, currentUserId());
        return ResponseEntity.noContent().build();
    }

    // ── Utilitaire prive ───────────────────────────────────────────────────

    /**
     * Extrait l'userId depuis le SecurityContext.
     * JwtAuthenticationFilter stocke l'userId dans les details du token.
     */
    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getDetails();
    }
}