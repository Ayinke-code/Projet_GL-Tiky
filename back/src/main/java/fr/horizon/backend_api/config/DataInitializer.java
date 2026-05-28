package fr.horizon.backend_api.config;

import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.model.enums.TeacherType;
import fr.horizon.backend_api.model.enums.RoleEnum;
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

        Role adminRole = getOrCreateRole(RoleEnum.ADMIN.value());
        Role teacherRole = getOrCreateRole(RoleEnum.TEACHER.value());
        Role studentRole = getOrCreateRole(RoleEnum.STUDENT.value());

        // User teacher et admin
        User admin = getOrCreateUser("admin@admin.fr", "Richard WOODWARD", Set.of(adminRole, teacherRole), null);
        // User teacher
        getOrCreateUser("teacher@teacher.fr", "Sophie ROUSSEAU", Set.of(teacherRole), TeacherType.COURSE_LEADER);
        getOrCreateUser("teacher2@teacher.fr", "Paul MARTIN", Set.of(teacherRole), TeacherType.OPTION_LEADER);
        getOrCreateUser("marie.dupont@teacher.fr", "Marie DUPONT", Set.of(teacherRole), TeacherType.COURSE_LEADER);
        getOrCreateUser("luc.bernard@teacher.fr", "Luc BERNARD", Set.of(teacherRole), TeacherType.OPTION_LEADER);
        getOrCreateUser("claire.moreau@teacher.fr", "Claire MOREAU", Set.of(teacherRole), TeacherType.COURSE_LEADER);
        getOrCreateUser("julien.petit@teacher.fr", "Julien PETIT", Set.of(teacherRole), TeacherType.OPTION_LEADER);
        getOrCreateUser("nathalie.simon@teacher.fr", "Nathalie SIMON", Set.of(teacherRole), TeacherType.COURSE_LEADER);
        getOrCreateUser("thomas.leroy@teacher.fr", "Thomas LEROY", Set.of(teacherRole), TeacherType.OPTION_LEADER);
        getOrCreateUser("isabelle.roux@teacher.fr", "Isabelle ROUX", Set.of(teacherRole), TeacherType.COURSE_LEADER);
        getOrCreateUser("pierre.garnier@teacher.fr", "Pierre GARNIER", Set.of(teacherRole), TeacherType.OPTION_LEADER);
        getOrCreateUser("anne.faure@teacher.fr", "Anne FAURE", Set.of(teacherRole), TeacherType.COURSE_LEADER);
        getOrCreateUser("nicolas.blanc@teacher.fr", "Nicolas BLANC", Set.of(teacherRole), TeacherType.OPTION_LEADER);
        getOrCreateUser("laura.henry@teacher.fr", "Laura HENRY", Set.of(teacherRole), TeacherType.COURSE_LEADER);
        getOrCreateUser("olivier.lambert@teacher.fr", "Olivier LAMBERT", Set.of(teacherRole), TeacherType.OPTION_LEADER);
        // User students (50 étudiants)
        User student = getOrCreateUser("student@student.fr", "Raoudate", Set.of(studentRole), null);
        getOrCreateUser("alice.martin@student.fr", "Alice MARTIN", Set.of(studentRole), null);
        getOrCreateUser("baptiste.dupont@student.fr", "Baptiste DUPONT", Set.of(studentRole), null);
        getOrCreateUser("camille.leroy@student.fr", "Camille LEROY", Set.of(studentRole), null);
        getOrCreateUser("damien.moreau@student.fr", "Damien MOREAU", Set.of(studentRole), null);
        getOrCreateUser("emma.bernard@student.fr", "Emma BERNARD", Set.of(studentRole), null);
        getOrCreateUser("florian.petit@student.fr", "Florian PETIT", Set.of(studentRole), null);
        getOrCreateUser("gabriel.thomas@student.fr", "Gabriel THOMAS", Set.of(studentRole), null);
        getOrCreateUser("hugo.robert@student.fr", "Hugo ROBERT", Set.of(studentRole), null);
        getOrCreateUser("inès.richard@student.fr", "Inès RICHARD", Set.of(studentRole), null);
        getOrCreateUser("jade.simon@student.fr", "Jade SIMON", Set.of(studentRole), null);
        getOrCreateUser("kevin.michel@student.fr", "Kevin MICHEL", Set.of(studentRole), null);
        getOrCreateUser("lea.lefebvre@student.fr", "Léa LEFEBVRE", Set.of(studentRole), null);
        getOrCreateUser("mathieu.garcia@student.fr", "Mathieu GARCIA", Set.of(studentRole), null);
        getOrCreateUser("nina.david@student.fr", "Nina DAVID", Set.of(studentRole), null);
        getOrCreateUser("oscar.bertrand@student.fr", "Oscar BERTRAND", Set.of(studentRole), null);
        getOrCreateUser("pauline.roux@student.fr", "Pauline ROUX", Set.of(studentRole), null);
        getOrCreateUser("quentin.vincent@student.fr", "Quentin VINCENT", Set.of(studentRole), null);
        getOrCreateUser("romane.fournier@student.fr", "Romane FOURNIER", Set.of(studentRole), null);
        getOrCreateUser("samuel.morel@student.fr", "Samuel MOREL", Set.of(studentRole), null);
        getOrCreateUser("thibault.girard@student.fr", "Thibault GIRARD", Set.of(studentRole), null);
        getOrCreateUser("ulysse.andre@student.fr", "Ulysse ANDRE", Set.of(studentRole), null);
        getOrCreateUser("victoire.blanc@student.fr", "Victoire BLANC", Set.of(studentRole), null);
        getOrCreateUser("william.guerin@student.fr", "William GUERIN", Set.of(studentRole), null);
        getOrCreateUser("xena.perrin@student.fr", "Xena PERRIN", Set.of(studentRole), null);
        getOrCreateUser("yann.faure@student.fr", "Yann FAURE", Set.of(studentRole), null);
        getOrCreateUser("zoe.rousseau@student.fr", "Zoé ROUSSEAU", Set.of(studentRole), null);
        getOrCreateUser("adrien.henry@student.fr", "Adrien HENRY", Set.of(studentRole), null);
        getOrCreateUser("beatrice.durand@student.fr", "Béatrice DURAND", Set.of(studentRole), null);
        getOrCreateUser("charlie.lambert@student.fr", "Charlie LAMBERT", Set.of(studentRole), null);
        getOrCreateUser("delphine.bonnet@student.fr", "Delphine BONNET", Set.of(studentRole), null);
        getOrCreateUser("edouard.francois@student.fr", "Edouard FRANCOIS", Set.of(studentRole), null);
        getOrCreateUser("fatima.legrand@student.fr", "Fatima LEGRAND", Set.of(studentRole), null);
        getOrCreateUser("gauthier.garnier@student.fr", "Gauthier GARNIER", Set.of(studentRole), null);
        getOrCreateUser("heloise.chevalier@student.fr", "Héloïse CHEVALIER", Set.of(studentRole), null);
        getOrCreateUser("igor.robin@student.fr", "Igor ROBIN", Set.of(studentRole), null);
        getOrCreateUser("joanna.muller@student.fr", "Joanna MULLER", Set.of(studentRole), null);
        getOrCreateUser("killian.lecomte@student.fr", "Killian LECOMTE", Set.of(studentRole), null);
        getOrCreateUser("laurie.mercier@student.fr", "Laurie MERCIER", Set.of(studentRole), null);
        getOrCreateUser("maxence.clement@student.fr", "Maxence CLEMENT", Set.of(studentRole), null);
        getOrCreateUser("noemie.gros@student.fr", "Noémie GROS", Set.of(studentRole), null);
        getOrCreateUser("olivier.colin@student.fr", "Olivier COLIN", Set.of(studentRole), null);
        getOrCreateUser("perrine.baudoin@student.fr", "Perrine BAUDOIN", Set.of(studentRole), null);
        getOrCreateUser("renaud.vidal@student.fr", "Renaud VIDAL", Set.of(studentRole), null);
        getOrCreateUser("sarah.bourgeois@student.fr", "Sarah BOURGEOIS", Set.of(studentRole), null);
        getOrCreateUser("theo.carpentier@student.fr", "Théo CARPENTIER", Set.of(studentRole), null);
        getOrCreateUser("ursula.savoy@student.fr", "Ursula SAVOY", Set.of(studentRole), null);
        getOrCreateUser("valentin.menard@student.fr", "Valentin MENARD", Set.of(studentRole), null);
        getOrCreateUser("wendy.barbier@student.fr", "Wendy BARBIER", Set.of(studentRole), null);
        getOrCreateUser("xavier.aubry@student.fr", "Xavier AUBRY", Set.of(studentRole), null);
        getOrCreateUser("yasmine.caron@student.fr", "Yasmine CARON", Set.of(studentRole), null);

        // =========================
        // CATEGORIES DEFAULT
        // =========================
        if (categoryRepository.count() == 0) {
            // Issues Categories

            createCategory("Maintenance & Infrastructure", "Maintenance", CategoryType.ISSUE, admin);
            createCategory("Scolarité & Administration", "Scolarité", CategoryType.ISSUE, admin);
            createCategory("Sécurité", "Sécurité", CategoryType.ISSUE, admin);
            createCategory("Vie étudiante", "Vie Étudiante", CategoryType.ISSUE, admin);
            createCategory("Pédagogie", "Pédagogie", CategoryType.ISSUE, admin);
            createCategory("Discrimination & Harcèlement", "Discrimination", CategoryType.ISSUE, admin);
            createCategory("Réseau & Informatique", "Réseau", CategoryType.ISSUE, admin);
            createCategory("Bibliothèque & Documentation", "Bibliothèque", CategoryType.ISSUE, admin);

            // Audit Categories
            createCategory("Test", "Audit Test", CategoryType.AUDIT, admin);
            createCategory("Base de données", "Audit BD", CategoryType.AUDIT, admin);
            createCategory("UI/UX", "Audit Interface", CategoryType.AUDIT, admin);
            createCategory("CI/CD", "Audit Pipeline", CategoryType.AUDIT, admin);
            createCategory("Qualité Code", "Audit Qualité", CategoryType.AUDIT, admin);
            createCategory("Modélisation et conception", "Audit Modélisation", CategoryType.AUDIT, admin);

            // Defense Categories
            createCategory("Soutenance stage technique", "Stage Technique", CategoryType.DEFENSE, admin);
            createCategory("Stage de fin d'étude", "Fin d'étude", CategoryType.DEFENSE, admin);
            createCategory("PFE", "Projet Fin Études", CategoryType.DEFENSE, admin);
            createCategory("Projet starter", "Starter", CategoryType.DEFENSE, admin);
            createCategory("PGL", "Génie Logiciel", CategoryType.DEFENSE, admin);
            createCategory("Sprint 1", "Sprint 1", CategoryType.DEFENSE, admin);
            createCategory("Sprint 2", "Sprint 2", CategoryType.DEFENSE, admin);
            createCategory("Sprint 3", "Sprint 3", CategoryType.DEFENSE, admin);
        }

        // Migration : catégories sans type → CategoryType.ISSUE
        categoryRepository.findAll().stream()
                .filter(c -> c.getCategoryType() == null)
                .forEach(c -> {
                    c.setCategoryType(CategoryType.ISSUE);
                    categoryRepository.save(c);
                });

        // =========================
        // CATEGORIES OPTION / PROJECT (idempotent)
        // =========================
        getOrCreateCategory("LD", "Logiciels Distribués", CategoryType.OPTION, admin);
        getOrCreateCategory("CSS", "Cybersécurité des Systèmes et des Softwares", CategoryType.OPTION, admin);
        getOrCreateCategory("SE", "Systèmes Embarqués", CategoryType.OPTION, admin);
        getOrCreateCategory("DISA", "Données & Intelligence des Systèmes d'Affaires", CategoryType.OPTION, admin);
        getOrCreateCategory("EOC", "Entrepreneuriat & Organisation des Entreprises Connectées", CategoryType.OPTION, admin);
        getOrCreateCategory("Big Data", "Big Data & Analytique", CategoryType.OPTION, admin);
        getOrCreateCategory("IA", "Intelligence Artificielle", CategoryType.OPTION, admin);
        getOrCreateCategory("Cybersécurité", "Cybersécurité Avancée", CategoryType.OPTION, admin);
        getOrCreateCategory("Énergie", "Systèmes Énergétiques", CategoryType.OPTION, admin);

        getOrCreateCategory("Horizon", "Projet Horizon", CategoryType.PROJECT, admin);
        getOrCreateCategory("Challenger", "Projet Challenger", CategoryType.PROJECT, admin);
        getOrCreateCategory("Sirena", "Projet Sirena", CategoryType.PROJECT, admin);
        getOrCreateCategory("Atlas", "Projet Atlas", CategoryType.PROJECT, admin);
        getOrCreateCategory("Nexus", "Projet Nexus", CategoryType.PROJECT, admin);

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
     * @param categoryType type de catégorie associé ({@code ISSUE}, {@code AUDIT}, {@code DEFENSE}...)
     * @param owner     utilisateur propriétaire de la catégorie
     */
    private void createCategory(String name, String subject, CategoryType categoryType, User owner) {
        categoryRepository.save(
                Category.builder()
                        .name(name)
                        .subject(subject)
                        .categoryType(categoryType)
                        .status(CategoryStatus.ACTIVE)
                        .owner(owner)
                        .build()
        );
    }

    private void getOrCreateCategory(String name, String subject, CategoryType categoryType, User owner) {
        if (!categoryRepository.existsByNameIgnoreCase(name)) {
            createCategory(name, subject, categoryType, owner);
        }
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
