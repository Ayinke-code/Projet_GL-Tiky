package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.repository.UserRepository;
import fr.horizon.backend_api.model.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtToken jwtToken;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName("ADMIN");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("admin@admin.fr");
        testUser.setName("Admin");
        testUser.setPassword("encodedPassword");
        testUser.setRole(adminRole);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@admin.fr");
        loginRequest.setPassword("password");
    }

    @Test
    void testLoginSuccess() {
        when(userRepository.findByEmail("admin@admin.fr")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtToken.generateToken("admin@admin.fr", 1L, "ADMIN")).thenReturn("jwtToken");

        var response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwtToken", response.getToken());
        verify(userRepository).findByEmail("admin@admin.fr");
        verify(passwordEncoder).matches("password", "encodedPassword");
    }

    @Test
    void testLoginUserNotFound() {
        when(userRepository.findByEmail("notfound@admin.fr")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("notfound@admin.fr");
        request.setPassword("password");

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    void testLoginInvalidPassword() {
        when(userRepository.findByEmail("admin@admin.fr")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
    }
}
