package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.model.dto.UserRequestDto;
import fr.horizon.backend_api.repository.RoleRepository;
import fr.horizon.backend_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

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
    void createUser_should_save_user_when_valid() {

        UserRequestDto dto = new UserRequestDto();
        dto.setName("John");
        dto.setEmail("john@test.com");
        dto.setPassword("pass");
        dto.setRoleId(1L);

        Role role = new Role();
        role.setId(1L);
        role.setName("USER");

        when(userRepository.existsByEmail("john@test.com"))
                .thenReturn(false);

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        when(passwordEncoder.encode("pass"))
                .thenReturn("encodedPass");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createUser(dto);

        assertNotNull(result);
        assertEquals("john@test.com", result.getEmail());
        assertEquals("encodedPass", result.getPassword());
    }

    @Test
    void createUser_should_throw_when_email_exists() {

        UserRequestDto dto = new UserRequestDto();
        dto.setEmail("john@test.com");

        when(userRepository.existsByEmail("john@test.com"))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> userService.createUser(dto));
    }

    @Test
    void createUser_should_throw_when_role_not_found() {

        UserRequestDto dto = new UserRequestDto();
        dto.setEmail("john@test.com");
        dto.setRoleId(1L);

        when(userRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(roleRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.createUser(dto));
    }

    @Test
    void findByEmail_should_return_user() {

        User user = new User();
        user.setEmail("john@test.com");

        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.of(user));

        User result = userService.findByEmail("john@test.com");

        assertEquals("john@test.com", result.getEmail());
    }

    @Test
    void findByEmail_should_throw_when_not_found() {

        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.findByEmail("john@test.com"));
    }

    @Test
    void toDto_should_map_user_correctly() {

        Role role = new Role();
        role.setName("USER");

        User user = User.builder()
                .id(1L)
                .name("John")
                .email("john@test.com")
                .role(role)
                .build();

        UserDto dto = userService.toDto(user);

        assertEquals(1L, dto.getId());
        assertEquals("John", dto.getName());
        assertEquals("john@test.com", dto.getEmail());
        assertEquals("USER", dto.getRole());
    }
}
