package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.horizon.backend_api.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    List<User> findDistinctByRoles_NameIn(List<String> roleNames);
    List<User> getAllByRoles(Set<Role> roles);
}
