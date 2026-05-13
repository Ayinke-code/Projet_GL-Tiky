package fr.horizon.backend_api.config;

import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.repository.*;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private IssueRepository issueRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationUserRepository conversationUserRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(dataInitializer, "defaultPassword", "password");
    }

    @Test
    void shouldInitializeDefaultData() {
        when(roleRepository.findByName(any())).thenReturn(Optional.empty());
        when(roleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User mockUser = User.builder().id(1L).email("admin@admin.fr").name("Admin").roles(Set.of()).build();
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(mockUser);

        when(passwordEncoder.encode(any())).thenReturn("encoded-password");

        when(categoryRepository.count()).thenReturn(0L);
        when(issueRepository.count()).thenReturn(0L);
        when(conversationRepository.count()).thenReturn(0L);

        Category mockCategory = Category.builder().id(1L).build();
        when(categoryRepository.findAll()).thenReturn(List.of(mockCategory));
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        when(issueRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        dataInitializer.run();

        verify(roleRepository, times(3)).save(any());
        verify(userRepository, times(3)).save(any());
        verify(categoryRepository, times(8)).save(any());
        verify(issueRepository).save(any());
    }
}
