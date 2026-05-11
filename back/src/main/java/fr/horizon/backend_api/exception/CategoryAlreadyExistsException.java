package fr.horizon.backend_api.exception;

/**
 * Exception levée quand une catégorie avec le même nom existe déjà.
 * 
 * @author Tiky Team
 * @version 1.0
 */
public class CategoryAlreadyExistsException extends RuntimeException {

    /**
     * Constructeur avec message.
     * 
     * @param message Le message d'erreur
     */
    public CategoryAlreadyExistsException(String message) {
        super(message);
    }

    /**
     * Constructeur avec message et cause.
     * 
     * @param message Le message d'erreur
     * @param cause   La cause de l'exception
     */
    public CategoryAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
