package fr.horizon.backend_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_KEY = "error";

    @ExceptionHandler(InvalidGroupSizeException.class)
    public ResponseEntity<Map<String, String>> handleInvalidGroupSize(InvalidGroupSizeException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(StudentAlreadyInGroupException.class)
    public ResponseEntity<Map<String, String>> handleStudentAlreadyInGroup(StudentAlreadyInGroupException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEventNotFound(EventNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(StudentGroupNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleStudentGroupNotFound(StudentGroupNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(EventConflictException.class)
    public ResponseEntity<Map<String, String>> handleEventConflict(EventConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    @ExceptionHandler(StorageSpaceFullException.class)
    public ResponseEntity<Map<String, String>> handleStorageFull(StorageSpaceFullException ex) {
        return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE) // 507
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    /**
     * Uploaded file in a format that is not accepted.
     *
     * @param ex exception carrying the rejected file name and the accepted formats
     * @return 415 with the user-facing message
     */
    @ExceptionHandler(UnsupportedFileFormatException.class)
    public ResponseEntity<Map<String, String>> handleUnsupportedFileFormat(UnsupportedFileFormatException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE) // 415
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    /**
     * Uploaded file (or batch) heavier than the application limits.
     *
     * @param ex exception carrying the limit that was exceeded
     * @return 413 with the user-facing message
     */
    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<Map<String, String>> handleFileTooLarge(FileTooLargeException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE) // 413
                .body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    /**
     * Request rejected by the servlet container itself (spring.servlet.multipart.* limits),
     * before any controller runs. Handled here so the client gets the same JSON shape
     * as for the application-level checks instead of the default error page.
     *
     * @param ex exception raised while parsing the multipart request
     * @return 413 with a user-facing message
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE) // 413
                .body(Map.of(ERROR_KEY, "Fichier trop volumineux : la taille maximale autorisée est dépassée"));
    }
}
