package fr.horizon.backend_api.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class JwtTokenTest {
    @Test
    void shouldGenerateAndParseToken() {
        JwtToken jwtToken = new JwtToken();
        ReflectionTestUtils.setField(jwtToken, "secret", "mysecretkeymysecretkeymysecretkey");
        ReflectionTestUtils.setField(jwtToken, "expiration", 10000L);

        String token = jwtToken.generateToken("test@mail.com", 1L, "ADMIN");

        Claims claims = jwtToken.parseToken(token);

        assertEquals("test@mail.com", claims.getSubject());
        assertEquals(1, claims.get("id"));
        assertEquals("ADMIN", claims.get("role"));
    }
}
