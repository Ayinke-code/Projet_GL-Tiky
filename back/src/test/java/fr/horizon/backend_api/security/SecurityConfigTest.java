package fr.horizon.backend_api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setup() {

        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);

        securityConfig = new SecurityConfig(jwtFilter);

        ReflectionTestUtils.setField(
                securityConfig,
                "allowedOrigins",
                List.of("http://localhost")
        );
    }

    @Test
    void shouldCreateCorsConfigurationSource() {

        CorsConfigurationSource source =
                securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();

        CorsConfiguration config =
                source.getCorsConfiguration(request);

        assertNotNull(config);

        assertTrue(
                config.getAllowedOriginPatterns()
                        .contains("http://localhost")
        );

        assertTrue(config.getAllowedMethods().contains("GET"));

        assertTrue(config.getAllowCredentials());
    }

    @Test
    void shouldCreateSecurityFilterChain() {

        assertNotNull(securityConfig);
    }
}
