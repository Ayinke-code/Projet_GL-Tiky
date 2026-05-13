package fr.horizon.backend_api.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordConfigTest {

    private final PasswordConfig passwordConfig = new PasswordConfig();

    @Test
    void shouldReturnBCryptPasswordEncoder() {

        PasswordEncoder encoder = passwordConfig.passwordEncoder();

        assertNotNull(encoder);

        String raw = "password";
        String encoded = encoder.encode(raw);

        assertTrue(encoder.matches(raw, encoded));
    }
}
