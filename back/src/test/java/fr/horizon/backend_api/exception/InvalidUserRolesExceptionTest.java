package fr.horizon.backend_api.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvalidUserRolesExceptionTest {

    @Test
    void shouldStoreMessage() {

        InvalidUserRolesException ex =
                new InvalidUserRolesException("Invalid roles");

        assertEquals("Invalid roles", ex.getMessage());
    }
}
