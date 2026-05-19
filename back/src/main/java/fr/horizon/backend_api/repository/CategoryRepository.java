package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'accès aux données de la catégorie.
 * Fournit les opérations CRUD et les requêtes personnalisées.
 *
 * @author Tiky Team
 * @version 1.0
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndSubjectIgnoreCase(String name, String subject);

    List<Category> findByStatus(CategoryStatus status);

    List<Category> findByOwner(User owner);

    List<Category> findByOwnerId(Long ownerId);

    List<Category> findByEventType(EventType eventType);

}