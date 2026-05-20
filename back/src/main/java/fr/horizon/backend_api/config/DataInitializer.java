package fr.horizon.backend_api.config;

import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.EventType;
import fr.horizon.backend_api.model.enums.TeacherType;
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

/**
 * Initialiseur de données exécuté au démarrage de l’application.
 * <p>
 * Crée les rôles, utilisateurs et catégories par défaut si ceux-ci
 * n’existent pas encore en base de données.
 * <p>
 * Utilise une stratégie d’upsert pour les utilisateurs : si un utilisateur
 * existe déjà (par email), son nom et son type enseignant sont mis à jour.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final IssueRepository issueRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-password}")
    private String defaultPassword;

    /**
     * Point d’entrée exécuté au démarrage de l’application.
     * <p>
     * Initialise en ordre : rôles &rarr; utilisateurs &rarr; catégories &rarr; issue de test.
     * Chaque étape est conditionnée à l’absence de données pour les catégories et issues.
     *
     * @param args arguments de démarrage (non utilisés)
     */
    @Override
    public void run(String... args) {

        Role adminRole = getOrCreateRole("ADMIN");
        Role teacherRole = getOrCreateRole("TEACHER");
        Role studentRole = getOrCreateRole("STUDENT");

        // User teacher et admin
        User admin = getOrCreateUser("admin@admin.fr", "Richard WOODWARD", Set.of(adminRole, teacherRole), null);
        // User teacher
        getOrCreateUser("teacher@teacher.fr", "Sophie ROUSSEAU", Set.of(teacherRole), TeacherType.COURSE_LEADER);
        getOrCreateUser("teacher2@teacher.fr", "Paul MARTIN", Set.of(teacherRole), TeacherType.OPTION_LEADER);
        // User student
        User student = getOrCreateUser("student@student.fr", "Raoudate", Set.of(studentRole), null);

        // =========================
        // CATEGORIES DEFAULT
        // =========================
        if (categoryRepository.count() == 0) {
            // Issues Categories

            createCategory("Maintenance & Infrastructure", "Maintenance",null, admin);
            createCategory("Scolarité & Administration", "Scolarité",null, admin);
            createCategory("Sécurité", "Sécurité",null, admin);
            createCategory("Vie étudiante", "Vie Étudiante",null, admin);
            createCategory("Pédagogie", "Pédagogie", null,admin);
            createCategory("Discrimination & Harcèlement", "Discrimination",null, admin);
            createCategory("Réseau & Informatique", "Réseau", null,admin);
            createCategory("Bibliothèque & Documentation", "Bibliothèque", null,admin);

            // Audit Categories
            createCategory("Test", "Audit Test", EventType.AUDIT, admin);
            createCategory("Base de données", "Audit BD", EventType.AUDIT, admin);
            createCategory("UI/UX", "Audit Interface", EventType.AUDIT, admin);
            createCategory("CI/CD", "Audit Pipeline", EventType.AUDIT, admin);
            createCategory("Qualité Code", "Audit Qualité", EventType.AUDIT, admin);
            createCategory("Modélisation et conception", "Audit Modélisation", EventType.AUDIT, admin);

            // Defense Categories
            createCategory("Soutenance stage technique", "Stage Technique", EventType.DEFENSE, admin);
            createCategory("Stage de fin d'étude", "Fin d'étude", EventType.DEFENSE, admin);
            createCategory("PFE", "Projet Fin Études", EventType.DEFENSE, admin);
            createCategory("Projet starter", "Starter", EventType.DEFENSE, admin);
            createCategory("PGL", "Génie Logiciel", EventType.DEFENSE, admin);
            createCategory("Sprint 1", "Sprint 1", EventType.DEFENSE, admin);
            createCategory("Sprint 2", "Sprint 2", EventType.DEFENSE, admin);
            createCategory("Sprint 3", "Sprint 3", EventType.DEFENSE, admin);
        }

        // =========================
        // ISSUE TEST
        // =========================
        if (issueRepository.count() == 0) {
            Category first = categoryRepository.findAll().get(0);
            createIssue("Climatisation en panne", "La climatisation de l'amphithéâtre A ne fonctionne plus depuis lundi", student, first);
        }
    }

    // =========================
    // HELPERS
    // =========================

    /**
     * Récupère un rôle existant ou le crée s’il est absent.
     *
     * @param name nom du rôle (é.g. {@code ADMIN}, {@code TEACHER}, {@code STUDENT})
     * @return rôle existant ou nouvellement créé
     */
    private Role getOrCreateRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    /**
     * Récupère un utilisateur existant (et le met à jour) ou le crée s’il est absent.
     * <p>
     * Si un utilisateur avec cet email existe déjà, son nom et son type enseignant
     * sont mis à jour et l’entité est persistée de nouveau.
     *
     * @param email       adresse email unique de l’utilisateur
     * @param name        nom complet
     * @param roles       ensemble des rôles affectés à la création (ignorés en cas de mise à jour)
     * @param teacherType type enseignant ({@code null} pour les non-enseignants)
     * @return utilisateur existant mis à jour ou nouvellement créé
     */
    private User getOrCreateUser(String email, String name, Set<Role> roles, TeacherType teacherType) {
        return userRepository.findByEmail(email)
                .map(existing -> {
                    existing.setName(name);
                    existing.setTeacherType(teacherType);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .name(name)
                                .password(passwordEncoder.encode(defaultPassword))
                                .roles(roles)
                                .teacherType(teacherType)
                                .build()
                ));
    }

    /**
     * Crée et persiste une catégorie avec le statut {@code ACTIVE}.
     *
     * @param name      nom de la catégorie
     * @param subject   sujet / libellé court
     * @param eventType type d’événement associé ({@code null} pour les catégories générales)
     * @param owner     utilisateur propriétaire de la catégorie
     */
    private void createCategory(String name, String subject, EventType eventType, User owner) {
        categoryRepository.save(
                Category.builder()
                        .name(name)
                        .subject(subject)
                        .eventType(eventType)
                        .status(CategoryStatus.ACTIVE)
                        .owner(owner)
                        .build()
        );
    }

    /**
     * Crée et persiste un incident de démonstration.
     *
     * @param title       titre de l’incident
     * @param description description détaillée
     * @param createdBy   utilisateur ayant signalé l’incident
     * @param category    catégorie rattachée
     */
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
