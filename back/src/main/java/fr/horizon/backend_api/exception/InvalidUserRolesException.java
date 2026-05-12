package fr.horizon.backend_api.exception;

public class InvalidUserRolesException extends RuntimeException {
    public InvalidUserRolesException(String message) {
        super(message);
    }
}
