package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'accès aux données de la catégorie.
 *
 * @author Tiky Team
 * @version 1.0
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name)")
    Optional<Category> findByNameIgnoreCase(@Param("name") String name);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c WHERE LOWER(c.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND LOWER(c.subject) = LOWER(:subject)")
    boolean existsByNameIgnoreCaseAndSubjectIgnoreCase(@Param("name") String name, @Param("subject") String subject);

    List<Category> findByStatus(CategoryStatus status);

    List<Category> findByOwner(User owner);

    List<Category> findByOwnerId(Long ownerId);

    List<Category> findByStatusAndOwner(CategoryStatus status, User owner);
}
