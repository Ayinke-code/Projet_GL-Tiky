package fr.horizon.backend_api.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenTest {

    private JwtToken jwtToken;

    @BeforeEach
    void setUp() {
        jwtToken = new JwtToken();
    }

    @Test
    @DisplayName("Générer et parser un token valide")
    void testGenerateAndParseToken() {
        String email = "test@example.com";
        Long id = 1L;
        String role = "ROLE_TEACHER";

        String token = jwtToken.generateToken(email, id, role);
        assertNotNull(token);

        Claims claims = jwtToken.parseToken(token);
        assertEquals(email, claims.getSubject());
        assertEquals(id.intValue(), claims.get("id")); // Jackson converts Long to Integer
        assertEquals(role, claims.get("role"));
    }

    @Test
    @DisplayName("Lancer une exception pour un token invalide")
    void testInvalidToken() {
        assertThrows(RuntimeException.class, () -> jwtToken.parseToken("invalid.token.here"));
    }
}
