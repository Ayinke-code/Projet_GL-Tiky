package fr.horizon.backend_api.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoleNotFoundExceptionTest {

    @Test
    void shouldHaveDefaultMessage() {

        RoleNotFoundException ex = new RoleNotFoundException();

        assertEquals("One or more roles not found", ex.getMessage());
    }
}
