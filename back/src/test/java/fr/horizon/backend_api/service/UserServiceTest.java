package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.InvalidUserRolesException;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.model.dto.UserRequestDto;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.RoleRepository;
import fr.horizon.backend_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IssueRepository issueRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_should_save_user_when_valid() {

        UserRequestDto dto = new UserRequestDto();
        dto.setName("John");
        dto.setEmail("john@test.com");
        dto.setPassword("pass");
        dto.setRoleIds(Set.of(1L));

        Role role = new Role();
        role.setId(1L);
        role.setName("USER");

        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(roleRepository.findAllById(Set.of(1L))).thenReturn(List.of(role));
        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = userService.createUser(dto);

        assertNotNull(result);
        assertEquals("john@test.com", result.getEmail());
        assertEquals("John", result.getName());
        assertEquals(Set.of("USER"), result.getRoles());
    }

    @Test
    void createUser_should_throw_when_email_exists() {

        UserRequestDto dto = new UserRequestDto();
        dto.setEmail("john@test.com");
        dto.setRoleIds(Set.of(1L));

        when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> userService.createUser(dto));
    }

    @Test
    void createUser_should_throw_when_role_not_found() {

        UserRequestDto dto = new UserRequestDto();
        dto.setEmail("john@test.com");
        dto.setRoleIds(Set.of(1L));

        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(roleRepository.findAllById(Set.of(1L))).thenReturn(List.of());

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
    void toDto_should_return_contributor_true_when_open_issues_exist() {

        Role role = new Role();
        role.setName("USER");

        User user = User.builder()
                .id(1L)
                .name("John")
                .email("john@test.com")
                .roles(Set.of(role))
                .build();

        when(issueRepository.existsByCreatedByIdAndStatus(1L, IssueStatus.OUVERT))
                .thenReturn(true);

        UserDto dto = userService.toDto(user);

        assertEquals(1L, dto.getId());
        assertEquals("John", dto.getName());
        assertEquals("john@test.com", dto.getEmail());
        assertEquals(Set.of("USER"), dto.getRoles());
        assertTrue(dto.isContributor());
    }

    @Test
    void toDto_should_return_contributor_false_when_no_open_issues() {

        Role role = new Role();
        role.setName("USER");

        User user = User.builder()
                .id(1L)
                .roles(Set.of(role))
                .build();

        when(issueRepository.existsByCreatedByIdAndStatus(1L, IssueStatus.OUVERT))
                .thenReturn(false);

        UserDto dto = userService.toDto(user);

        assertFalse(dto.isContributor());
    }

    @Test
    void findAll_should_return_users() {

        Role role = new Role();
        role.setName("USER");

        User user = User.builder()
                .id(1L)
                .name("John")
                .email("john@test.com")
                .roles(Set.of(role))
                .build();

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(issueRepository.existsByCreatedByIdAndStatus(1L, IssueStatus.OUVERT))
                .thenReturn(false);

        List<UserDto> result = userService.findAll();

        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getName());
    }

    @Test
    void findAllByRoles_should_return_users() {

        Role role = new Role();
        role.setName("TEACHER");

        User user = User.builder()
                .id(1L)
                .name("John")
                .roles(Set.of(role))
                .build();

        when(roleRepository.findByNameIn(List.of("TEACHER")))
                .thenReturn(List.of(role));

        when(userRepository.findDistinctByRoles_NameIn(List.of("TEACHER")))
                .thenReturn(List.of(user));

        List<?> result = userService.findAllByRoles(List.of("TEACHER"));

        assertEquals(1, result.size());
    }

    @Test
    void findAllByRoles_should_throw_when_role_invalid() {

        when(roleRepository.findByNameIn(List.of("FAKE")))
                .thenReturn(List.of());

        Executable executable = () -> userService.findAllByRoles(List.of("FAKE"));

        assertThrows(IllegalArgumentException.class, executable);
    }

    @Test
    void deleteById_should_call_repository() {

        userService.deleteById(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void createUser_should_throw_when_roles_empty() {

        UserRequestDto dto = new UserRequestDto();
        dto.setEmail("john@test.com");
        dto.setRoleIds(Set.of());

        assertThrows(InvalidUserRolesException.class,
                () -> userService.createUser(dto));
    }
}