package fr.horizon.backend_api.config;

import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.RoleRepository;
import fr.horizon.backend_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @BeforeEach
    void setup() {

        ReflectionTestUtils.setField(
                dataInitializer,
                "defaultPassword",
                "password"
        );
    }

    @Test
    void shouldInitializeDefaultData() {

        // GIVEN
        when(roleRepository.findByName(any()))
                .thenReturn(Optional.empty());

        when(roleRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.empty());

        when(userRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(passwordEncoder.encode(any()))
                .thenReturn("encoded-password");

        when(categoryRepository.count())
                .thenReturn(0L);

        when(issueRepository.count())
                .thenReturn(0L);

        Category category = Category.builder()
                .id(1L)
                .name("DEV")
                .build();

        when(categoryRepository.findAll())
                .thenReturn(List.of(category));

        // WHEN
        dataInitializer.run();

        // THEN
        verify(roleRepository, times(3)).save(any());
        verify(userRepository, times(66)).save(any());
        verify(categoryRepository, times(37)).save(any());

        verify(issueRepository).save(any());
    }

    @Test
    void shouldUpdateExistingUsersAndRoles() {

        // GIVEN — rôles déjà existants → getOrCreateRole prend le chemin "existant"
        Role adminRole = new Role("ADMIN");
        Role teacherRole = new Role("TEACHER");
        Role studentRole = new Role("STUDENT");

        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(studentRole));

        // Utilisateurs déjà existants → getOrCreateUser prend le chemin "upsert"
        User existingAdmin   = User.builder().id(1L).name("Old Admin").email("admin@admin.fr").build();
        User existingTeacher = User.builder().id(2L).name("Old Teacher").email("teacher@teacher.fr").build();
        User existingTeacher2 = User.builder().id(3L).name("Old Teacher2").email("teacher2@teacher.fr").build();
        User existingStudent = User.builder().id(4L).name("Old Student").email("student@student.fr").build();

        when(userRepository.findByEmail("admin@admin.fr")).thenReturn(Optional.of(existingAdmin));
        when(userRepository.findByEmail("teacher@teacher.fr")).thenReturn(Optional.of(existingTeacher));
        when(userRepository.findByEmail("teacher2@teacher.fr")).thenReturn(Optional.of(existingTeacher2));
        when(userRepository.findByEmail("student@student.fr")).thenReturn(Optional.of(existingStudent));
        // 12 nouveaux teachers → nouveaux utilisateurs
        when(userRepository.findByEmail("marie.dupont@teacher.fr")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("luc.bernard@teacher.fr")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("claire.moreau@teacher.fr")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("julien.petit@teacher.fr")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nathalie.simon@teacher.fr")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("thomas.leroy@teacher.fr")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("isabelle.roux@teacher.fr")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("pierre.garnier@teacher.fr")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("anne.faure@teacher.fr")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nicolas.blanc@teacher.fr")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("laura.henry@teacher.fr")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("olivier.lambert@teacher.fr")).thenReturn(Optional.empty());

        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Catégories et issues déjà existantes → skip création
        when(categoryRepository.count()).thenReturn(1L);
        when(issueRepository.count()).thenReturn(1L);

        // WHEN
        dataInitializer.run();

        // THEN — les rôles n'ont pas été re-créés
        verify(roleRepository, never()).save(any());

        // Les utilisateurs ont été créés ou mis à jour (save appelé pour chacun)
        verify(userRepository, times(66)).save(any());

        // Les noms ont bien été mis à jour
        assertEquals("Richard WOODWARD", existingAdmin.getName());
        assertEquals("Sophie ROUSSEAU", existingTeacher.getName());
    }
}
