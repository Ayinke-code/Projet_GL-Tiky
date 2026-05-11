package fr.horizon.backend_api.config;

import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.IssueRepository;
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
    private CategoryRepository categoryRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    @DisplayName("Devrait initialiser les données si elles n'existent pas")
    void run_InitializesData() {
        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(roleRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> {
            User savedUser = i.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });
        when(categoryRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenAnswer(i -> {
            Category savedCategory = i.getArgument(0);
            savedCategory.setId(1L);
            return savedCategory;
        });
        when(issueRepository.findAll()).thenReturn(java.util.List.of());
        when(issueRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        dataInitializer.run();

        verify(roleRepository, times(3)).save(any());
        verify(userRepository, times(5)).save(any());
    }

    @Test
    @DisplayName("Ne devrait pas recréer l'admin s'il existe")
    void run_AlreadyInitialized() {
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(new Role("SOME_ROLE")));
        User existingUser = new User();
        existingUser.setId(1L);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(existingUser));

        Category existingCategory = new Category();
        existingCategory.setId(1L);
        when(categoryRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(existingCategory));

        Issue existingIssue = new Issue();
        existingIssue.setIdTick(1L);
        existingIssue.setTitre("Climatisation en panne");
        existingIssue.setStatut(IssueStatus.OUVERT);
        existingIssue.setCategorie(existingCategory);
        when(issueRepository.findAll()).thenReturn(java.util.List.of(existingIssue));

        dataInitializer.run();

        verify(roleRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }
}
