package fr.horizon.backend_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.horizon.backend_api.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}
