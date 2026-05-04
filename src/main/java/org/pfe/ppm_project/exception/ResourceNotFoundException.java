package org.pfe.ppm_project.exception;

/**
 * Exception levee lorsqu'une ressource demandee n'existe pas en base de donnees.
 * Interceptee par GlobalExceptionHandler pour retourner un HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    // Constructeur avec nom de l'entite et identifiant
    public ResourceNotFoundException(String entity, Long id) {
        super(entity + " not found with id: " + id);
    }

    // Constructeur avec message personnalise
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
