package fr.horizon.backend_api.security;

import fr.horizon.backend_api.service.JwtToken;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtToken jwtToken;

    @Mock
    private Claims claims;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateFromBearerToken() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer fake-token");

        when(jwtToken.parseToken("fake-token"))
                .thenReturn(claims);

        when(claims.getSubject())
                .thenReturn("test@test.com");

        when(claims.get("roles", List.class))
                .thenReturn(List.of("ADMIN"));

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        assertEquals(
                "test@test.com",
                SecurityContextHolder.getContext().getAuthentication().getName()
        );

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateFromCookie() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setCookies(new Cookie("token", "cookie-token"));

        when(jwtToken.parseToken("cookie-token"))
                .thenReturn(claims);

        when(claims.getSubject())
                .thenReturn("student@test.com");

        when(claims.get("roles", List.class))
                .thenReturn(List.of("STUDENT"));

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        assertEquals(
                "student@test.com",
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
    }

    @Test
    void shouldClearContextWhenTokenInvalid() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer invalid");

        when(jwtToken.parseToken("invalid"))
                .thenThrow(new RuntimeException());

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueFilterChainWhenNoToken() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldHandleNullRoles() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer fake-token");

        when(jwtToken.parseToken("fake-token"))
                .thenReturn(claims);

        when(claims.getSubject())
                .thenReturn("user@test.com");

        when(claims.get("roles", List.class))
                .thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);
    }
}
