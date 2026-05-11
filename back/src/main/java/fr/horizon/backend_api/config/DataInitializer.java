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

    private Category getOrCreateCategory(String name, String subject, User owner) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Category cat = Category.builder()
                            .name(name)
                            .subject(subject)
                            .status("active")
                            .categoryOwner(String.valueOf(owner.getId()))
                            .build();
                    return categoryRepository.save(cat);
                });
    }

    private void createIssueIfMissing(String titre, String description, Category category, User createur) {
        boolean exists = issueRepository.findAll().stream()
                .anyMatch(i -> titre.equalsIgnoreCase(i.getTitre())
                        && i.getCategorie() != null
                        && category.getId().equals(i.getCategorie().getId()));

        if (!exists) {
            Issue issue = new Issue();
            issue.setTitre(titre);
            issue.setDescription(description);
            issue.setStatut(IssueStatus.OUVERT);
            issue.setEstPrive(false);
            issue.setCategorie(category);
            issue.setCreateur(createur);
            issueRepository.save(issue);
        }
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

        // Créer les catégories manquantes (seed idempotent)
        Category maintenance = getOrCreateCategory("Maintenance & Infrastructure", "Maintenance", teacher1);
        Category scolarite = getOrCreateCategory("Scolarité & Administration", "Scolarité", teacher2);
        getOrCreateCategory("Sécurité", "Sécurité", teacher1);
        getOrCreateCategory("Vie étudiante", "Vie Étudiante", teacher2);
        getOrCreateCategory("Pédagogie", "Pédagogie", teacher1);
        getOrCreateCategory("Discrimination & Harcèlement", "Discrimination", teacher2);
        getOrCreateCategory("Réseau & Informatique", "Réseau", teacher1);
        getOrCreateCategory("Bibliothèque & Documentation", "Bibliothèque", teacher2);

        // Créer les issues manquantes, même si la base contient déjà des catégories
        createIssueIfMissing(
            "Climatisation en panne",
            "La climatisation de l'amphithéâtre A n'a plus de climatisation depuis lundi",
            maintenance,
            student1);

        createIssueIfMissing(
            "Vidéoprojecteur salle B HS",
            "Le vidéoprojecteur ne s'allume plus depuis ce matin",
            maintenance,
            student2);

        createIssueIfMissing(
            "Erreur sur mon relevé de notes",
            "La note du devoir 2 n'apparait pas sur mon relevé semestriel",
            scolarite,
            student1);

        createIssueIfMissing(
            "Carte étudiante non reçue",
            "Je n'ai toujours pas reçu ma carte étudiante alors que l'inscription est validée",
            scolarite,
            student2);

    }
}
