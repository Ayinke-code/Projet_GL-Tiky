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

        if (!userRepository.existsByEmail("admin@admin.fr")) {
            User admin = new User();
            admin.setEmail("admin@admin.fr");
            admin.setName("Admin");
            admin.setPassword(passwordEncoder.encode("password"));
            admin.setRole(adminRole);

            userRepository.save(admin);
        }

    }
}
