package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.LoginRequest;
import fr.horizon.backend_api.model.dto.LoginResponse;
import fr.horizon.backend_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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

    private User user;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().id(1L).name("ROLE_TEACHER").build();
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .name("John Doe")
                .role(role)
                .build();

        loginRequest = new LoginRequest("test@example.com", "password123");
    }

    @Test
    @DisplayName("Login avec succès")
    void login_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtToken.generateToken("test@example.com", 1L, "ROLE_TEACHER")).thenReturn("mock-jwt-token");

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("John Doe", response.getName());
        assertEquals("ROLE_TEACHER", response.getRole());
    }

    @Test
    @DisplayName("Login avec email inexistant")
    void login_EmailNotFound() {
        when(userRepository.findByEmail("wrong@example.com")).thenReturn(Optional.empty());

        loginRequest = new LoginRequest("wrong@example.com", "password");

        assertThrows(ResponseStatusException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Login avec mauvais mot de passe")
    void login_WrongPassword() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        loginRequest = new LoginRequest("test@example.com", "wrongPassword");

        assertThrows(ResponseStatusException.class, () -> authService.login(loginRequest));
    }
}
