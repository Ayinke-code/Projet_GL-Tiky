package fr.horizon.backend_api.exception;

public class IssueNotFoundException extends RuntimeException {
    public IssueNotFoundException(Long id) {
        super("Issue not found with id: " + id);
    }
}
