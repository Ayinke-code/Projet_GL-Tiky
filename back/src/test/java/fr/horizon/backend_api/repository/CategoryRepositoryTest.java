package fr.horizon.backend_api.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import fr.horizon.backend_api.model.Category;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour le repository CategoryRepository.
 * Teste les opérations CRUD et les requêtes personnalisées.
 * 
 * @author Tiky Team
 * @version 1.0
 */
@DataJpaTest
@DisplayName("Tests d'intégration - CategoryRepository")
class CategoryRepositoryTest {

    /**
     * Repository à tester.
     */
    @Autowired
    private CategoryRepository categoryRepository;

    private Category category1;
    private Category category2;
    private String teacherId = "teacher-001";

    /**
     * Configuration avant chaque test.
     */
    @BeforeEach
    void setUp() {
        category1 = Category.builder()
                .name("Mathématiques")
                .status("active")
                .categoryOwner(teacherId)
                .build();

        category2 = Category.builder()
                .name("Physique")
                .status("active")
                .categoryOwner("teacher-002")
                .build();

        categoryRepository.deleteAll();
    }

    /**
     * Test : Sauvegarder une catégorie en base de données.
     * 
     * Résultat attendu :
     * - La catégorie est sauvegardée avec un ID généré
     */
    @Test
    @DisplayName("Doit sauvegarder une catégorie en base de données")
    void testSaveCategory() {
        // Act
        Category saved = categoryRepository.save(category1);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("Mathématiques", saved.getName());
        assertEquals("active", saved.getStatus());
    }

    /**
     * Test : Trouver une catégorie par son nom (insensible à la casse).
     * 
     * Conditions :
     * - Une catégorie existe
     * 
     * Résultat attendu :
     * - La catégorie est trouvée avec différentes casses
     */
    @Test
    @DisplayName("Doit trouver une catégorie par son nom (insensible à la casse)")
    void testFindByNameIgnoreCase() {
        // Arrange
        categoryRepository.save(category1);

        // Act - Recherche avec différentes casses
        Optional<Category> found1 = categoryRepository.findByNameIgnoreCase("mathématiques");
        Optional<Category> found2 = categoryRepository.findByNameIgnoreCase("MATHÉMATIQUES");
        Optional<Category> found3 = categoryRepository.findByNameIgnoreCase("Mathématiques");

        // Assert
        assertTrue(found1.isPresent());
        assertTrue(found2.isPresent());
        assertTrue(found3.isPresent());
        assertEquals("Mathématiques", found1.get().getName());
    }

    /**
     * Test : Vérifier l'existence d'une catégorie par son nom (insensible à la
     * casse).
     * 
     * Conditions :
     * - Une catégorie existe
     * 
     * Résultat attendu :
     * - La vérification retourne true pour le nom exact et en minuscules/majuscules
     */
    @Test
    @DisplayName("Doit vérifier l'existence d'une catégorie par son nom")
    void testExistsByNameIgnoreCase() {
        // Arrange
        categoryRepository.save(category1);

        // Act & Assert
        assertTrue(categoryRepository.existsByNameIgnoreCase("Mathématiques"));
        assertTrue(categoryRepository.existsByNameIgnoreCase("mathématiques"));
        assertTrue(categoryRepository.existsByNameIgnoreCase("MATHÉMATIQUES"));
        assertFalse(categoryRepository.existsByNameIgnoreCase("Chimie"));
    }

    /**
     * Test : Récupérer toutes les catégories d'un enseignant.
     * 
     * Conditions :
     * - L'enseignant a 2 catégories
     * 
     * Résultat attendu :
     * - Les 2 catégories sont retournées
     */
    @Test
    @DisplayName("Doit récupérer toutes les catégories d'un enseignant")
    void testFindByCategoryOwner() {
        // Arrange
        Category category3 = Category.builder()
                .name("Histoire")
                .status("active")
                .categoryOwner(teacherId)
                .build();

        categoryRepository.save(category1);
        categoryRepository.save(category2);
        categoryRepository.save(category3);

        // Act
        List<Category> found = categoryRepository.findByCategoryOwner(teacherId);

        // Assert
        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(c -> "Mathématiques".equals(c.getName())));
        assertTrue(found.stream().anyMatch(c -> "Histoire".equals(c.getName())));
    }

    /**
     * Test : Récupérer les catégories actives d'un enseignant.
     * 
     * Conditions :
     * - L'enseignant a 1 catégorie active et 1 inactive
     * 
     * Résultat attendu :
     * - Seule la catégorie active est retournée
     */
    @Test
    @DisplayName("Doit récupérer les catégories actives d'un enseignant")
    void testFindByStatusAndCategoryOwner() {
        // Arrange
        Category inactiveCategory = Category.builder()
                .name("Chimie")
                .status("inactive")
                .categoryOwner(teacherId)
                .build();

        categoryRepository.save(category1);
        categoryRepository.save(inactiveCategory);

        // Act
        List<Category> found = categoryRepository.findByStatusAndCategoryOwner("active", teacherId);

        // Assert
        assertEquals(1, found.size());
        assertEquals("Mathématiques", found.get(0).getName());
        assertEquals("active", found.get(0).getStatus());
    }

    /**
     * Test : Récupérer toutes les catégories avec un statut spécifique.
     * 
     * Conditions :
     * - 2 catégories actives et 1 inactive
     * 
     * Résultat attendu :
     * - Les 2 catégories actives sont retournées
     */
    @Test
    @DisplayName("Doit récupérer toutes les catégories avec un statut spécifique")
    void testFindByStatus() {
        // Arrange
        Category inactiveCategory = Category.builder()
                .name("Chimie")
                .status("inactive")
                .categoryOwner("teacher-003")
                .build();

        categoryRepository.save(category1);
        categoryRepository.save(category2);
        categoryRepository.save(inactiveCategory);

        // Act
        List<Category> active = categoryRepository.findByStatus("active");
        List<Category> inactive = categoryRepository.findByStatus("inactive");

        // Assert
        assertEquals(2, active.size());
        assertEquals(1, inactive.size());
        assertTrue(active.stream().allMatch(c -> "active".equals(c.getStatus())));
    }

    /**
     * Test : Supprimer une catégorie.
     * 
     * Résultat attendu :
     * - La catégorie est supprimée
     */
    @Test
    @DisplayName("Doit supprimer une catégorie")
    void testDeleteCategory() {
        // Arrange
        Category saved = categoryRepository.save(category1);
        Long id = saved.getId();

        // Act
        categoryRepository.deleteById(id);

        // Assert
        assertFalse(categoryRepository.existsById(id));
    }

    /**
     * Test : Mise à jour d'une catégorie.
     * 
     * Résultat attendu :
     * - La catégorie est mise à jour
     */
    @Test
    @DisplayName("Doit mettre à jour une catégorie")
    void testUpdateCategory() {
        // Arrange
        Category saved = categoryRepository.save(category1);

        // Act
        saved.setStatus("inactive");
        Category updated = categoryRepository.save(saved);

        // Assert
        assertEquals("inactive", updated.getStatus());
        assertEquals("Mathématiques", updated.getName());
    }

    /**
     * Test : Contrainte d'unicité sur le nom.
     * 
     * Conditions :
     * - Une catégorie existe
     * 
     * Résultat attendu :
     * - La sauvegarde d'une catégorie avec le même nom lève une exception
     */
    @Test
    @DisplayName("Doit respecter la contrainte d'unicité sur le nom")
    void testUniqueNameConstraint() {
        // Arrange
        categoryRepository.save(category1);

        Category duplicate = Category.builder()
                .name("Mathématiques")
                .status("active")
                .categoryOwner("teacher-002")
                .build();

        // Act & Assert
        assertThrows(Exception.class, () -> categoryRepository.save(duplicate));
    }
}
