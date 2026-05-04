package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.LoginRequest;
import fr.horizon.backend_api.model.dto.LoginResponse;
import fr.horizon.backend_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void login_should_return_login_response_when_credentials_valid() {

        // GIVEN
        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setName("Test");
        user.setPassword("encoded");
        Role role = new Role();
        role.setName("USER");
        user.setRole(role);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");

        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password", "encoded"))
                .thenReturn(true);

        when(jwtToken.generateToken("test@test.com", 1L, "USER"))
                .thenReturn("fake-jwt-token");

        // WHEN
        LoginResponse response = authService.login(request);

        // THEN
        assertNotNull(response);
        assertEquals("fake-jwt-token", response.getToken());
        assertEquals("test@test.com", response.getEmail());
    }

    @Test
    void login_should_throw_exception_when_password_invalid() {

        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("encoded");

        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(any(), any()))
                .thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("wrong");

        assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
    }
}