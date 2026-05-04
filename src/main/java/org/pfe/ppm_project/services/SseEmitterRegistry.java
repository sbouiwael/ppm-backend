package org.pfe.ppm_project.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registre thread-safe des connexions SSE actives par utilisateur.
 *
 * Chaque utilisateur peut avoir plusieurs onglets ouverts — le registre maintient
 * une liste de SseEmitters par userId.  Quand un evenement est publie pour un
 * utilisateur, tous ses emitters actifs recevront l'evenement.
 *
 * NETTOYAGE : les emitters expires ou en erreur sont supprimes automatiquement
 * via les callbacks onCompletion / onTimeout / onError.
 */
@Component
@Slf4j
public class SseEmitterRegistry {

    /**
     * Timeout par defaut : 5 minutes.
     * Le navigateur reconnectera automatiquement (comportement EventSource natif).
     */
    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1_000L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Cree et enregistre un nouveau SseEmitter pour l'utilisateur donne.
     * Retourne l'emitter a renvoyer dans la reponse HTTP.
     */
    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        log.debug("[SSE] Registered emitter for userId={}", userId);

        // Nettoyage quand la connexion se ferme (normalement ou en erreur)
        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> {
            log.debug("[SSE] Emitter error for userId={}: {}", userId, ex.getMessage());
            cleanup.run();
        });

        // Envoie un commentaire de type "ping" pour confirmer la connexion
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Envoie un evenement JSON a tous les emitters actifs de l'utilisateur.
     * Les emitters qui echouent lors de l'envoi sont retires silencieusement.
     *
     * @param userId    destinataire
     * @param eventName nom de l'evenement SSE (ex: "notification")
     * @param data      objet serialise en JSON par Spring
     */
    public void push(Long userId, String eventName, Object data) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) return;

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.debug("[SSE] Failed to push to userId={}, removing emitter", userId);
                dead.add(emitter);
            }
        }
        userEmitters.removeAll(dead);
    }

    /** Retourne le nombre de connexions actives pour un utilisateur (pour les tests). */
    public int countConnections(Long userId) {
        List<SseEmitter> list = emitters.get(userId);
        return list == null ? 0 : list.size();
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emitters.remove(userId);
            log.debug("[SSE] Removed emitter for userId={}", userId);
        }
    }
}
