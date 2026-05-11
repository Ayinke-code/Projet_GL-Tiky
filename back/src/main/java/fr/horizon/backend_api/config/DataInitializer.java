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

    @Override
    public void run(String... args) {

        getOrCreateRole("STUDENT");
        getOrCreateRole("TEACHER");
        Role adminRole = getOrCreateRole("ADMIN");

        User admin = null;
        if (!userRepository.existsByEmail("admin@admin.fr")) {
            admin = new User();
            admin.setEmail("admin@admin.fr");
            admin.setName("Admin");
            admin.setPassword(passwordEncoder.encode("password"));
            admin.setRole(adminRole);
            admin = userRepository.save(admin);
        } else {
            admin = userRepository.findByEmail("admin@admin.fr").orElse(null);
        }

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
                Category cat = Category.builder()
                        .name(categories[i])
                        .subject(subjects[i])
                        .status("active")
                        .categoryOwner(String.valueOf(admin.getId()))
                        .build();
                categoryRepository.save(cat);
            }

            // Créer une issue de test
            Category firstCategory = categoryRepository.findAll().get(0);
            Issue testIssue = new Issue();
            testIssue.setTitre("Climatisation en panne");
            testIssue.setDescription("La climatisation de l'amphithéâtre A n'a plus de climatisation depuis lundi");
            testIssue.setStatut(IssueStatus.OUVERT);
            testIssue.setEstPrive(false);
            testIssue.setCategorie(firstCategory);
            testIssue.setCreateur(admin);
            issueRepository.save(testIssue);
        }

    }
}
