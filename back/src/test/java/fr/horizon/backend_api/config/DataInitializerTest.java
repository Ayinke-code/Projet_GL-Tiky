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
