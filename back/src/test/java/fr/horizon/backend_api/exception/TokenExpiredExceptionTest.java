package fr.horizon.backend_api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenExpiredExceptionTest {

    @Test
    void shouldStoreMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");

        TokenExpiredException ex = new TokenExpiredException("token expiré", cause);

        assertEquals("token expiré", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void shouldBeRuntimeException() {
        TokenExpiredException ex = new TokenExpiredException("msg", new Exception());
        assertInstanceOf(RuntimeException.class, ex);
    }
}
