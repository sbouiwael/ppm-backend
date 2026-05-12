package org.pfe.ppm_project.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions pour l'API REST.
 * Intercepte toutes les exceptions levees par les controleurs
 * et les transforme en reponses HTTP structurees (status, message, timestamp).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Gere les exceptions de ressource non trouvee -> HTTP 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Gere les exceptions d'operation invalide -> HTTP 400
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOp(InvalidOperationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Gere les erreurs de validation des DTOs (@Valid) -> HTTP 400 avec liste des erreurs
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, errors);
    }

    // Gere les erreurs d'acces refuse (Spring Security) -> HTTP 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.debug("Access denied: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied");
    }

    // Gere les violations de contraintes d'integrite (ex: email duplique) -> HTTP 409
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = "Data integrity violation";
        String cause = ex.getMostSpecificCause().getMessage();
        if (cause != null && cause.toLowerCase().contains("email")) {
            message = "Email already in use";
        } else if (cause != null && cause.toLowerCase().contains("duplicate")) {
            message = "Duplicate entry — this record already exists";
        }
        return buildResponse(HttpStatus.CONFLICT, message);
    }

    /**
     * Gere les timeouts des connexions SSE (Server-Sent Events).
     * AsyncRequestTimeoutException est levee normalement quand une connexion SSE expire
     * (ex: apres 5 minutes de silence). Le client EventSource reconnecte automatiquement.
     * On ne loggue pas en ERROR car c'est un comportement attendu.
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Void> handleAsyncTimeout(AsyncRequestTimeoutException ex) {
        log.debug("[SSE] Async request timed out (client will reconnect): {}", ex.getMessage());
        return ResponseEntity.noContent().build();
    }

    /**
     * Gere les erreurs d'ecriture de message HTTP.
     * Peut se produire quand le GlobalExceptionHandler tente d'ecrire une reponse JSON
     * sur une connexion SSE (text/event-stream) deja committee.
     * Dans ce cas on laisse Spring gerer la fermeture de la connexion.
     */
    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<Void> handleMessageNotWritable(HttpMessageNotWritableException ex) {
        log.debug("[SSE] HttpMessageNotWritable — probable SSE response already committed: {}", ex.getMessage());
        return ResponseEntity.noContent().build();
    }

    // Gere toutes les autres exceptions non prevues -> HTTP 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    // Construit une reponse d'erreur standardisee avec status, message et timestamp
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
