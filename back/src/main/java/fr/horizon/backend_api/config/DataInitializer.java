package fr.horizon.backend_api.config;

import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.repository.RoleRepository;
import fr.horizon.backend_api.repository.UserRepository;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final IssueRepository issueRepository;
    private final PasswordEncoder passwordEncoder;

    private Role getOrCreateRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    private User getOrCreateUser(String email, String name, String rawPassword, Role role) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail(email);
                    user.setName(name);
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    user.setRole(role);
                    return userRepository.save(user);
                });
    }

    @Override
    public void run(String... args) {

        Role studentRole = getOrCreateRole("STUDENT");
        Role teacherRole = getOrCreateRole("TEACHER");
        Role adminRole = getOrCreateRole("ADMIN");

        User admin = getOrCreateUser("admin@admin.fr", "Admin", "password", adminRole);
        User teacher1 = getOrCreateUser("teacher1@horizon.fr", "Prof Martin", "password", teacherRole);
        User teacher2 = getOrCreateUser("teacher2@horizon.fr", "Dr Smith", "password", teacherRole);
        User student1 = getOrCreateUser("student1@horizon.fr", "Alice Student", "password", studentRole);
        User student2 = getOrCreateUser("student2@horizon.fr", "Bob Student", "password", studentRole);

        // Créer les catégories si elles n'existent pas
        if (categoryRepository.count() == 0 && admin != null) {
            String[] categories = {
                "Maintenance & Infrastructure",
                "Scolarité & Administration",
                "Sécurité",
                "Vie étudiante",
                "Pédagogie",
                "Discrimination & Harcèlement",
                "Réseau & Informatique",
                "Bibliothèque & Documentation"
            };

            String[] subjects = {
                "Maintenance",
                "Scolarité",
                "Sécurité",
                "Vie Étudiante",
                "Pédagogie",
                "Discrimination",
                "Réseau",
                "Bibliothèque"
            };

            for (int i = 0; i < categories.length; i++) {
                User owner = (i % 2 == 0) ? teacher1 : teacher2;
                Category cat = Category.builder()
                        .name(categories[i])
                        .subject(subjects[i])
                        .status("active")
                        .categoryOwner(String.valueOf(owner.getId()))
                        .build();
                categoryRepository.save(cat);
            }

            // Créer des issues de test (Issue Creator = createur)
            Category firstCategory = categoryRepository.findAll().get(0);
            Issue testIssue = new Issue();
            testIssue.setTitre("Climatisation en panne");
            testIssue.setDescription("La climatisation de l'amphithéâtre A n'a plus de climatisation depuis lundi");
            testIssue.setStatut(IssueStatus.OUVERT);
            testIssue.setEstPrive(false);
            testIssue.setCategorie(firstCategory);
            testIssue.setCreateur(student1);
            issueRepository.save(testIssue);

            Issue secondIssue = new Issue();
            secondIssue.setTitre("Vidéoprojecteur salle B HS");
            secondIssue.setDescription("Le vidéoprojecteur ne s'allume plus depuis ce matin");
            secondIssue.setStatut(IssueStatus.OUVERT);
            secondIssue.setEstPrive(false);
            secondIssue.setCategorie(firstCategory);
            secondIssue.setCreateur(student2);
            issueRepository.save(secondIssue);
        }

    }
}
