package org.pfe.ppm_project.exception;

/**
 * Exception levee lorsqu'une operation metier est invalide
 * (ex: doublon d'affectation, depassement de charge, auto-dependance).
 * Interceptee par GlobalExceptionHandler pour retourner un HTTP 400.
 */
public class InvalidOperationException extends RuntimeException {

    // Constructeur avec message decrivant la raison de l'invalidite
    public InvalidOperationException(String message) {
        super(message);
    }
}
