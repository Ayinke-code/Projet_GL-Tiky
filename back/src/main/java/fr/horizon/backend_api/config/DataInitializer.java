package fr.horizon.backend_api.config;

import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.ConversationRole;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.repository.*;
import lombok.extern.slf4j.Slf4j;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.repository.RoleRepository;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final IssueRepository issueRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationUserRepository conversationUserRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-password}")
    private String defaultPassword;

    @Override
    public void run(String... args) {

        Role adminRole = getOrCreateRole("ADMIN");
        Role teacherRole = getOrCreateRole("TEACHER");
        Role studentRole = getOrCreateRole("STUDENT");

        // User teacher et admin
        User admin = getOrCreateUser("admin@admin.fr", "Admin", Set.of(adminRole, teacherRole));
        // User teacher
        getOrCreateUser("teacher@teacher.fr", "Teacher", Set.of(teacherRole));
        // User student
        User student = getOrCreateUser("student@student.fr", "Student", Set.of(studentRole));

        // =========================
        // CATEGORIES DEFAULT
        // =========================
        if (categoryRepository.count() == 0) {

            createCategory("Maintenance & Infrastructure", "Maintenance", admin);
            createCategory("Scolarité & Administration", "Scolarité", admin);
            createCategory("Sécurité", "Sécurité", admin);
            createCategory("Vie étudiante", "Vie Étudiante", admin);
            createCategory("Pédagogie", "Pédagogie", admin);
            createCategory("Discrimination & Harcèlement", "Discrimination", admin);
            createCategory("Réseau & Informatique", "Réseau", admin);
            createCategory("Bibliothèque & Documentation", "Bibliothèque", admin);
        }

        // =========================
        // ISSUE TEST
        // =========================
        if (issueRepository.count() == 0) {

            Category first = categoryRepository.findAll().get(0);
            createIssue("Climatisation en panne", "La climatisation de l'amphithéâtre A ne fonctionne plus depuis lundi", student, first);
        }

        // =========================
        // CONVERSATION TEST
        // =========================
        if (conversationRepository.count() == 0 && issueRepository.count() > 0) {
            Issue testIssue = issueRepository.findAll().get(0);
            User teacher = userRepository.findByEmail("teacher@teacher.fr").orElse(null);

            if (teacher != null) {
                // Create conversation
                Conversation conversation = Conversation.builder()
                        .issue(testIssue)
                        .build();
                conversation = conversationRepository.save(conversation);

                // Add users to conversation
                ConversationUser initiator = ConversationUser.builder()
                        .conversation(conversation)
                        .user(admin)
                        .role(ConversationRole.INITIATOR)
                        .build();
                conversationUserRepository.save(initiator);

                ConversationUser responder = ConversationUser.builder()
                        .conversation(conversation)
                        .user(teacher)
                        .role(ConversationRole.RESPONDER)
                        .build();
                conversationUserRepository.save(responder);

                // Add test messages
                Message message1 = Message.builder()
                        .content("Le problème a été signalé lundi matin, la climatisation ne fonctionne plus depuis.")
                        .user(admin)
                        .conversation(conversation)
                        .issue(testIssue)
                        .build();
                messageRepository.save(message1);

                Message message2 = Message.builder()
                        .content("Nous avons confié la réparation à l'équipe de maintenance. Ils interviennent demain à 10h.")
                        .user(teacher)
                        .conversation(conversation)
                        .issue(testIssue)
                        .build();
                messageRepository.save(message2);

                Message message3 = Message.builder()
                        .content("Merci pour votre réactivité! Nous informerons les étudiants du planning.")
                        .user(admin)
                        .conversation(conversation)
                        .issue(testIssue)
                        .build();
                messageRepository.save(message3);
            }
        }
    }

    // =========================
    // HELPERS
    // =========================

    private Role getOrCreateRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    private User getOrCreateUser(String email, String name, Set<Role> roles) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .name(name)
                                .password(passwordEncoder.encode(defaultPassword))
                                .roles(roles)
                                .build()
                ));
    }

    private void createCategory(String name, String subject, User owner) {
        categoryRepository.save(
                Category.builder()
                        .name(name)
                        .subject(subject)
                        .status(CategoryStatus.ACTIVE)
                        .owner(owner)
                        .build()
        );
    }

    private void createIssue(String title, String description, User createdBy, Category category) {
        issueRepository.save(
                Issue.builder()
                        .title(title)
                        .description(description)
                        .createdBy(createdBy)
                        .category(category)
                        .build()
        );
    }
}
