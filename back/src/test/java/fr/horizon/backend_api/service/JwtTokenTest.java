package fr.horizon.backend_api.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class JwtTokenTest {

    @Test
    void shouldGenerateAndParseToken() {

        JwtToken jwtToken = new JwtToken();

        ReflectionTestUtils.setField(jwtToken, "secret", "mysecretkeymysecretkeymysecretkey");
        ReflectionTestUtils.setField(jwtToken, "expiration", 10000L);

        Set<String> roles = Set.of("ADMIN");

        String token = jwtToken.generateToken("test@mail.com", 1L, roles);

        Claims claims = jwtToken.parseToken(token);

        assertEquals("test@mail.com", claims.getSubject());
        assertEquals(1, claims.get("id"));

        assertEquals(roles, Set.copyOf((List<String>) claims.get("roles")));
    }
}
