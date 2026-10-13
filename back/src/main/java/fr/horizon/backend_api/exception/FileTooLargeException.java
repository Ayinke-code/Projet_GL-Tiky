package fr.horizon.backend_api.exception;

/**
 * Thrown when an uploaded file, or a batch of files, exceeds the weight limits.
 * Mapped to HTTP 413 by {@link GlobalExceptionHandler}.
 */
public class FileTooLargeException extends RuntimeException {

    /**
     * @param message user-facing message that states the limit that was exceeded
     */
    public FileTooLargeException(String message) {
        super(message);
    }
}
