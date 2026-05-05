package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.repository.RoleRepository;
import fr.horizon.backend_api.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Créer un utilisateur avec succès")
    void createUser_Success() {
        UserDto dto = new UserDto();
        dto.setName("Test User");
        dto.setEmail("test@user.com");
        dto.setPassword("pass");
        dto.setRoleId(1L);

        Role role = Role.builder().id(1L).name("ROLE_TEACHER").build();

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        User result = userService.createUser(dto);

        assertNotNull(result);
        assertEquals("Test User", result.getName());
        assertEquals("encoded", result.getPassword());
    }

    @Test
    @DisplayName("Lancer une exception si email déjà utilisé")
    void createUser_EmailUsed() {
        UserDto dto = new UserDto();
        dto.setEmail("used@test.com");

        when(userRepository.existsByEmail("used@test.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.createUser(dto));
    }
}
