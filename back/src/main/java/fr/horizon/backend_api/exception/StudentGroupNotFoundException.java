package fr.horizon.backend_api.exception;

public class StudentGroupNotFoundException extends RuntimeException {
    public StudentGroupNotFoundException(Long id) {
        super("StudentGroup not found with id: " + id);
    }
}
