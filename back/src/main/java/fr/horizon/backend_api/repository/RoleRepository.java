package fr.horizon.backend_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.horizon.backend_api.model.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
