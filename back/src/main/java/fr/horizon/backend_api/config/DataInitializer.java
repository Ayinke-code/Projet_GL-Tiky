package fr.horizon.backend_api.config;

import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import fr.horizon.backend_api.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RequestManagerRepository requestManagerRepository;
    private final CategoryRepository categoryRepository;
    private final IssueRepository issueRepository;
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
        getOrCreateUser("student@student.fr", "Student", Set.of(studentRole));

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
        Issue testIssue;

        if (issueRepository.count() == 0) {
            // Si aucune issue n'existe, on en crée une
            Category first = categoryRepository.findAll().get(0);

            testIssue = Issue.builder()
                    .title("Climatisation en panne")
                    .description("La climatisation de l'amphithéâtre A ne fonctionne plus depuis lundi")
                    .status(IssueStatus.OUVERT)
                    .isPrivate(false)
                    .category(first)
                    .createdBy(admin)
                    .build();

            testIssue = issueRepository.save(testIssue); // On récupère l'issue sauvegardée (avec son ID)
        } else {
            // Si elles existent déjà, on en récupère une pour le test du RequestManager
            testIssue = issueRepository.findAll().get(0);
        }

        // 3. Initialisation du RequestManager
        if (requestManagerRepository.count() == 0) {
            RequestManager request = RequestManager.builder()
                    .issue(testIssue) // On utilise l'issue créée ou récupérée juste au-dessus
                    .categoryOwner(admin)
                    .status(RequestManagerStatus.PENDING)
                    .build();

            requestManagerRepository.save(request);
            log.info("Données de test initialisées : RequestManager créé pour l'issue '{}'", testIssue.getTitle());
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
}
