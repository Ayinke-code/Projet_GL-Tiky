package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.InvalidUserRolesException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.model.dto.UserFollowerDto;
import fr.horizon.backend_api.model.dto.UserRequestDto;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.model.enums.TeacherType;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.RoleRepository;
import fr.horizon.backend_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.persistence.EntityNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;

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

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private UserService userService;

    @Mock
    private JwtToken jwtToken;

    @BeforeEach
    void setUp() {
        lenient().when(jwtToken.generateToken(any(), any(), anySet())).thenReturn("ws-token");
    }

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
        assertEquals("ws-token", result.getWsToken());
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
        assertEquals("ws-token", dto.getWsToken());
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
        assertEquals("ws-token", dto.getWsToken());
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

    @Test
    void toDto_should_include_teacherType_when_user_is_teacher() {

        Role role = new Role();
        role.setName("TEACHER");

        User user = User.builder()
                .id(5L)
                .name("Sophie ROUSSEAU")
                .email("teacher@teacher.fr")
                .roles(Set.of(role))
                .teacherType(TeacherType.COURSE_LEADER)
                .build();

        when(issueRepository.existsByCreatedByIdAndStatus(5L, IssueStatus.OUVERT))
                .thenReturn(false);

        UserDto dto = userService.toDto(user);

        assertEquals(TeacherType.COURSE_LEADER, dto.getTeacherType());
        assertEquals("Sophie ROUSSEAU", dto.getName());
    }

    @Test
    void toDto_should_have_null_teacherType_when_user_is_student() {

        Role role = new Role();
        role.setName("STUDENT");

        User user = User.builder()
                .id(6L)
                .name("Raoudate")
                .email("student@student.fr")
                .roles(Set.of(role))
                .teacherType(null)
                .build();

        when(issueRepository.existsByCreatedByIdAndStatus(6L, IssueStatus.OUVERT))
                .thenReturn(false);

        UserDto dto = userService.toDto(user);

        assertNull(dto.getTeacherType());
    }

    @Test
    void createUser_should_save_teacherType_when_provided() {

        UserRequestDto dto = new UserRequestDto();
        dto.setName("Paul MARTIN");
        dto.setEmail("teacher2@teacher.fr");
        dto.setPassword("pass");
        dto.setRoleIds(Set.of(2L));
        dto.setTeacherType(TeacherType.OPTION_LEADER);

        Role role = new Role();
        role.setId(2L);
        role.setName("TEACHER");

        when(userRepository.existsByEmail("teacher2@teacher.fr")).thenReturn(false);
        when(roleRepository.findAllById(Set.of(2L))).thenReturn(List.of(role));
        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(issueRepository.existsByCreatedByIdAndStatus(any(), any())).thenReturn(false);

        UserDto result = userService.createUser(dto);

        assertEquals(TeacherType.OPTION_LEADER, result.getTeacherType());
        assertEquals("Paul MARTIN", result.getName());
    }

    @Test
    void getAllTeachersWithCategoryDetail_should_mark_followers_correctly() {
        Role teacherRole = new Role("TEACHER");
        teacherRole.setId(1L);

        User teacher1 = new User();
        teacher1.setId(10L);
        teacher1.setName("Alice");

        User teacher2 = new User();
        teacher2.setId(11L);
        teacher2.setName("Bob");

        Category category = new Category();
        category.setId(5L);
        category.setName("Option IA");
        category.setFollowers(Set.of(teacher1));

        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(userRepository.getAllByRoles(Set.of(teacherRole))).thenReturn(List.of(teacher1, teacher2));

        List<UserFollowerDto> result = userService.getAllTeachersWithCategoryDetail(5L);

        assertEquals(2, result.size());
        UserFollowerDto dto1 = result.stream().filter(d -> d.getId().equals(10L)).findFirst().orElseThrow();
        UserFollowerDto dto2 = result.stream().filter(d -> d.getId().equals(11L)).findFirst().orElseThrow();
        assertTrue(dto1.isFollower());
        assertFalse(dto2.isFollower());
    }

    @Test
    void getAllTeachersWithCategoryDetail_should_throw_when_teacher_role_not_found() {
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.getAllTeachersWithCategoryDetail(1L));
    }

    @Test
    void getAllTeachersWithCategoryDetail_should_throw_when_category_not_found() {
        Role teacherRole = new Role("TEACHER");
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.getAllTeachersWithCategoryDetail(99L));
    }
}