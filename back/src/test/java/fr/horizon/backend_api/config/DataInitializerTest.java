package fr.horizon.backend_api.config;

import fr.horizon.backend_api.model.Category;
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

        when(categoryRepository.findAll())
                .thenReturn(List.of(Category.builder().build()));

        // WHEN
        dataInitializer.run();

        // THEN
        verify(roleRepository, times(3)).save(any());

        verify(userRepository, times(3)).save(any());

        verify(categoryRepository, times(8)).save(any());

        verify(issueRepository).save(any());
    }
}

package fr.horizon.backend_api.config;

import fr.horizon.backend_api.model.Role;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    @DisplayName("Devrait initialiser les données si elles n'existent pas")
    void run_InitializesData() {
        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(roleRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(userRepository.existsByEmail("admin@admin.fr")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        dataInitializer.run();

        verify(roleRepository, times(3)).save(any());
        verify(userRepository).save(any());
    }

    @Test
    @DisplayName("Ne devrait pas recréer l'admin s'il existe")
    void run_AlreadyInitialized() {
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(new Role("SOME_ROLE")));
        when(userRepository.existsByEmail("admin@admin.fr")).thenReturn(true);

        dataInitializer.run();

        verify(roleRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }
}
