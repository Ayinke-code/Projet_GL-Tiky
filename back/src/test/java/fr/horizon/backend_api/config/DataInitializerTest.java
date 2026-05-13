package fr.horizon.backend_api.config;

import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.RoleRepository;
import fr.horizon.backend_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.anyString;
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
        ReflectionTestUtils.setField(dataInitializer, "defaultPassword", "password");
    }

    @Test
    @DisplayName("Should initialize default data if none exists")
    void shouldInitializeDefaultData() {
        // GIVEN
        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(roleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(categoryRepository.count()).thenReturn(0L);
        when(issueRepository.count()).thenReturn(0L);
        when(categoryRepository.findAll()).thenReturn(List.of(Category.builder().build()));

        // WHEN
        dataInitializer.run();

        // THEN
        verify(roleRepository, times(3)).save(any());
        verify(userRepository, times(3)).save(any());
        verify(categoryRepository, times(8)).save(any());
        verify(issueRepository).save(any());
    }

    @Test
    @DisplayName("Should not recreate admin if it already exists")
    void shouldNotRecreateAdminIfExists() {
        // GIVEN
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(new Role("ADMIN")));
        when(userRepository.existsByEmail("admin@admin.fr")).thenReturn(true);

        // WHEN
        dataInitializer.run();

        // THEN
        verify(roleRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }
}
