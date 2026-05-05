package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.repository.UserRepository;
import fr.horizon.backend_api.repository.RoleRepository;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role testRole;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        testRole = new Role("STUDENT");
        testRole.setId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.fr");
        testUser.setName("Test User");
        testUser.setRole(testRole);

        userDto = new UserDto();
        userDto.setEmail("test@test.fr");
        userDto.setName("Test User");
        userDto.setPassword("password");
        userDto.setRoleId(1L);
    }

    @Test
    void testCreateUserSuccess() {
        when(userRepository.existsByEmail("test@test.fr")).thenReturn(false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
        when(passwordEncoder.encode("password")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User createdUser = userService.createUser(userDto);

        assertNotNull(createdUser);
        assertEquals("test@test.fr", createdUser.getEmail());
        verify(userRepository).existsByEmail("test@test.fr");
        verify(roleRepository).findById(1L);
        verify(passwordEncoder).encode("password");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUserEmailAlreadyExists() {
        when(userRepository.existsByEmail("test@test.fr")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.createUser(userDto));
        verify(userRepository).existsByEmail("test@test.fr");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUserRoleNotFound() {
        when(userRepository.existsByEmail("test@test.fr")).thenReturn(false);
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        userDto.setRoleId(999L);
        assertThrows(RuntimeException.class, () -> userService.createUser(userDto));
    }
}
