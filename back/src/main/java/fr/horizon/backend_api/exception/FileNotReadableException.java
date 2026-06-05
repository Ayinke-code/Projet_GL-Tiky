package fr.horizon.backend_api.exception;

public class FileNotReadableException extends RuntimeException {
    public FileNotReadableException() {
        super("File not readable");
    }
}
