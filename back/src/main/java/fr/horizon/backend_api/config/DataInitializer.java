package fr.horizon.backend_api.config;

import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.repository.RoleRepository;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private Role getOrCreateRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    @Override
    public void run(String... args) {

        Role studentRole = getOrCreateRole("STUDENT");
        Role teacherRole = getOrCreateRole("TEACHER");
        Role adminRole = getOrCreateRole("ADMIN");

        // Un compte admin
        if (!userRepository.existsByEmail("admin@admin.fr")) {
            User admin = new User();
            admin.setEmail("admin@admin.fr");
            admin.setName("Admin");
            admin.setPassword(passwordEncoder.encode("password"));
            admin.setRole(adminRole);

            userRepository.save(admin);
        }

        // Un compte professeur
        if (!userRepository.existsByEmail("teacher@teacher.fr")) {
            User teacher = new User();
            teacher.setEmail("teacher@teacher.fr");
            teacher.setName("Teacher");
            teacher.setPassword(passwordEncoder.encode("password"));
            teacher.setRole(teacherRole);

            userRepository.save(teacher);
        }

        // Un compte étudiant
        if (!userRepository.existsByEmail("student@student.fr")) {
            User student = new User();
            student.setEmail("student@student.fr");
            student.setName("Student");
            student.setPassword(passwordEncoder.encode("password"));
            student.setRole(studentRole);

            userRepository.save(student);
        }

    }
}
