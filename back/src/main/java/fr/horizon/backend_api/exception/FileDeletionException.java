package fr.horizon.backend_api.exception;

public class FileDeletionException extends RuntimeException {

    public FileDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
