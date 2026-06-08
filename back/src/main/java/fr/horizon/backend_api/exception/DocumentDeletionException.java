package fr.horizon.backend_api.exception;

public class DocumentDeletionException extends RuntimeException {

    public DocumentDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
