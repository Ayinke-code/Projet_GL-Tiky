package fr.horizon.backend_api.exception;

/**
 * Thrown when an uploaded file is not in one of the accepted formats.
 * Mapped to HTTP 415 by {@link GlobalExceptionHandler}.
 */
public class UnsupportedFileFormatException extends RuntimeException {

    /**
     * @param filename       original name of the rejected file (may be null when no file was sent)
     * @param allowedFormats human-readable list of accepted formats, repeated in the message
     *                       so the user knows what to send instead
     */
    public UnsupportedFileFormatException(String filename, String allowedFormats) {
        super("Format non autorisé : " + (filename == null ? "fichier sans nom" : filename)
                + " (formats acceptés : " + allowedFormats + ")");
    }
}
