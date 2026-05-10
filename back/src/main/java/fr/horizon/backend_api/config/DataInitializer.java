package fr.horizon.backend_api.config;

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

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_ADMIN = "ADMIN";

    private static final String ADMIN_EMAIL = "admin@admin.fr";
    private static final String TEACHER_EMAIL = "teacher@teacher.fr";
    private static final String STUDENT_EMAIL = "student@student.fr";

    @Value("${app.default-password}")
    private String defaultPassword;

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private Role getOrCreateRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    private User getOrCreateUser(String email, String name, Set<Role> roles) {

        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User user = User.builder()
                            .email(email)
                            .name(name)
                            .password(passwordEncoder.encode(defaultPassword))
                            .roles(roles)
                            .build();

                    return userRepository.save(user);
                });
    }

    @Override
    public void run(String... args) {

        Role studentRole = getOrCreateRole(ROLE_STUDENT);
        Role teacherRole = getOrCreateRole(ROLE_TEACHER);
        Role adminRole = getOrCreateRole(ROLE_ADMIN);

        getOrCreateUser(ADMIN_EMAIL, "Admin", Set.of(adminRole, teacherRole));
        getOrCreateUser(TEACHER_EMAIL, "Teacher", Set.of(teacherRole));
        getOrCreateUser(STUDENT_EMAIL, "Student", Set.of(studentRole));
    }
}
