package fr.horizon.backend_api.exception;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException() {
        super("One or more roles not found");
    }
}
