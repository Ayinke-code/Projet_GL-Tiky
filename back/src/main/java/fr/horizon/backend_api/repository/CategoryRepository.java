package fr.horizon.backend_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.horizon.backend_api.model.Category;

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

    /**
     * Recherche une catégorie par son nom (insensible à la casse).
     * 
     * @param name Le nom de la catégorie
     * @return Une catégorie optionnelle
     */
    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name)")
    Optional<Category> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Vérifie si une catégorie existe par son nom (insensible à la casse).
     * 
     * @param name Le nom de la catégorie
     * @return true si la catégorie existe, false sinon
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c WHERE LOWER(c.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    /**
     * Vérifie si une catégorie existe par son nom et sujet (insensible à la casse).
     * 
     * @param name    Le nom de la catégorie
     * @param subject Le sujet de la catégorie
     * @return true si la catégorie existe, false sinon
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND LOWER(c.subject) = LOWER(:subject)")
    boolean existsByNameIgnoreCaseAndSubjectIgnoreCase(@Param("name") String name, @Param("subject") String subject);

    /**
     * Récupère toutes les catégories actives d'un enseignant.
     * 
     * @param categoryOwner L'identifiant de l'enseignant
     * @return Liste des catégories actives
     */
    List<Category> findByStatusAndCategoryOwner(String status, String categoryOwner);

    /**
     * Récupère toutes les catégories dont l'enseignant est propriétaire.
     * 
     * @param categoryOwner L'identifiant de l'enseignant
     * @return Liste des catégories
     */
    List<Category> findByCategoryOwner(String categoryOwner);

    /**
     * Récupère toutes les catégories actives.
     * 
     * @return Liste des catégories actives
     */
    List<Category> findByStatus(String status);
}
