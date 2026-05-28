package fr.horizon.backend_api.exception;

public class MessageTooLongException extends RuntimeException {
    public MessageTooLongException(int maxLength) {
        super("Le message dépasse la limite de " + maxLength + " caractères");
    }
}

