package fr.horizon.backend_api.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmailAlreadyUsedExceptionTest {

    @Test
    void shouldBuildCorrectMessage() {

        EmailAlreadyUsedException ex =
                new EmailAlreadyUsedException("test@test.com");

        assertEquals("Email already used: test@test.com", ex.getMessage());
    }
}
