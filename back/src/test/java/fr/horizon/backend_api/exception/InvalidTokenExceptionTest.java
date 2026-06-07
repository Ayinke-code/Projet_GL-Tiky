package fr.horizon.backend_api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidTokenExceptionTest {

    @Test
    void shouldStoreMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");

        InvalidTokenException ex = new InvalidTokenException("token invalide", cause);

        assertEquals("token invalide", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void shouldBeRuntimeException() {
        InvalidTokenException ex = new InvalidTokenException("msg", new Exception());
        assertInstanceOf(RuntimeException.class, ex);
    }
}
