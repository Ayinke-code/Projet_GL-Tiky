package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.dto.CategoryResponseDTO;
import fr.horizon.backend_api.model.dto.CreateCategoryDTO;
import fr.horizon.backend_api.exception.CategoryAlreadyExistsException;
import fr.horizon.backend_api.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour le service CategoryService.
 * Teste la création, la récupération et la suppression des catégories.
 * 
 * @author Tiky Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - CategoryService")
class CategoryServiceTest {

    /**
     * Mock du repository pour les catégories.
     */
    @Mock
    private CategoryRepository categoryRepository;

    /**
     * Service à tester (injecté avec les mocks).
     */
    @InjectMocks
    private CategoryService categoryService;

    private CreateCategoryDTO createCategoryDTO;
    private Category mockCategory;
    private String teacherId = "teacher-001";

    /**
     * Configuration avant chaque test.
     */
    @BeforeEach
    void setUp() {
        createCategoryDTO = CreateCategoryDTO.builder()
                .name("Mathématiques")
                .subject("Science")
                .build();

        mockCategory = Category.builder()
                .id(1L)
                .name("Mathématiques")
                .subject("Science")
                .status("active")
                .categoryOwner(teacherId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Test : Créer une catégorie avec succès.
     * 
     * Conditions :
     * - Le nom n'existe pas déjà
     * - Le nom est valide
     * 
     * Résultat attendu :
     * - La catégorie est créée
     * - Le statut est "active"
     * - L'enseignant est assigné comme propriétaire
     */
    @Test
    @DisplayName("Doit créer une catégorie avec succès")
    void testCreateCategory_Success() {
        // Arrange
        when(categoryRepository.existsByNameIgnoreCaseAndSubjectIgnoreCase(anyString(), anyString())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(mockCategory);

        // Act
        CategoryResponseDTO result = categoryService.createCategory(createCategoryDTO, teacherId);

        // Assert
        assertNotNull(result);
        assertEquals("Mathématiques", result.getName());
        assertEquals("active", result.getStatus());
        assertEquals(teacherId, result.getOwner());
        assertEquals(1L, result.getId());

        // Vérifier que les méthodes du mock ont été appelées
        verify(categoryRepository, times(1)).existsByNameIgnoreCaseAndSubjectIgnoreCase("Mathématiques", "Science");
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    /**
     * Test : Échouer lors de la création si le nom existe déjà.
     * 
     * Conditions :
     * - Une catégorie avec le même nom existe déjà
     * 
     * Résultat attendu :
     * - Une exception CategoryAlreadyExistsException est levée
     */
    @Test
    @DisplayName("Doit lever une exception si la catégorie existe déjà")
    void testCreateCategory_AlreadyExists() {
        // Arrange
        when(categoryRepository.existsByNameIgnoreCaseAndSubjectIgnoreCase(anyString(), anyString())).thenReturn(true);

        // Act & Assert
        CategoryAlreadyExistsException exception = assertThrows(
                CategoryAlreadyExistsException.class,
                () -> categoryService.createCategory(createCategoryDTO, teacherId));

        assertTrue(exception.getMessage().contains("existe déjà"));
        verify(categoryRepository, times(1)).existsByNameIgnoreCaseAndSubjectIgnoreCase("Mathématiques", "Science");
        verify(categoryRepository, never()).save(any());
    }

    /**
     * Test : Échouer lors de la création si le nom est vide.
     * 
     * Conditions :
     * - Le nom est vide ou null
     * 
     * Résultat attendu :
     * - Une IllegalArgumentException est levée
     */
    @Test
    @DisplayName("Doit lever une exception si le nom est vide")
    void testCreateCategory_EmptyName() {
        // Arrange
        CreateCategoryDTO emptyDto = CreateCategoryDTO.builder()
                .name("")
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.createCategory(emptyDto, teacherId));

        assertTrue(exception.getMessage().contains("vide"));
        verify(categoryRepository, never()).save(any());
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
    void testGetCategoriesByTeacher() {
        // Arrange
        Category category2 = Category.builder()
                .id(2L)
                .name("Physique")
                .status("active")
                .categoryOwner(teacherId)
                .createdAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findByCategoryOwner(teacherId))
                .thenReturn(Arrays.asList(mockCategory, category2));

        // Act
        List<CategoryResponseDTO> results = categoryService.getCategoriesByTeacher(teacherId);

        // Assert
        assertEquals(2, results.size());
        assertEquals("Mathématiques", results.get(0).getName());
        assertEquals("Physique", results.get(1).getName());
        verify(categoryRepository, times(1)).findByCategoryOwner(teacherId);
    }

    /**
     * Test : Récupérer toutes les catégories actives d'un enseignant.
     * 
     * Conditions :
     * - L'enseignant a 2 catégories actives
     * 
     * Résultat attendu :
     * - Les 2 catégories actives sont retournées
     */
    @Test
    @DisplayName("Doit récupérer toutes les catégories actives d'un enseignant")
    void testGetActiveCategoriesByTeacher() {
        // Arrange
        when(categoryRepository.findByStatusAndCategoryOwner("active", teacherId))
                .thenReturn(Arrays.asList(mockCategory));

        // Act
        List<CategoryResponseDTO> results = categoryService.getActiveCategoriesByTeacher(teacherId);

        // Assert
        assertEquals(1, results.size());
        assertEquals("Mathématiques", results.get(0).getName());
        assertEquals("active", results.get(0).getStatus());
        verify(categoryRepository, times(1)).findByStatusAndCategoryOwner("active", teacherId);
    }

    /**
     * Test : Récupérer une catégorie par son ID.
     * 
     * Conditions :
     * - La catégorie existe
     * 
     * Résultat attendu :
     * - La catégorie est retournée
     */
    @Test
    @DisplayName("Doit récupérer une catégorie par son ID")
    void testGetCategoryById() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));

        // Act
        Optional<CategoryResponseDTO> result = categoryService.getCategoryById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Mathématiques", result.get().getName());
        verify(categoryRepository, times(1)).findById(1L);
    }

    /**
     * Test : Récupérer une catégorie par son nom (insensible à la casse).
     * 
     * Conditions :
     * - La catégorie existe
     * 
     * Résultat attendu :
     * - La catégorie est retournée
     */
    @Test
    @DisplayName("Doit récupérer une catégorie par son nom (insensible à la casse)")
    void testGetCategoryByName() {
        // Arrange
        when(categoryRepository.findByNameIgnoreCase("MATHÉMATIQUES"))
                .thenReturn(Optional.of(mockCategory));

        // Act
        Optional<CategoryResponseDTO> result = categoryService.getCategoryByName("MATHÉMATIQUES");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Mathématiques", result.get().getName());
        verify(categoryRepository, times(1)).findByNameIgnoreCase("MATHÉMATIQUES");
    }

    /**
     * Test : Supprimer une catégorie avec succès.
     * 
     * Conditions :
     * - La catégorie existe
     * 
     * Résultat attendu :
     * - La catégorie est supprimée
     */
    @Test
    @DisplayName("Doit supprimer une catégorie avec succès")
    void testDeleteCategory_Success() {
        // Arrange
        when(categoryRepository.existsById(1L)).thenReturn(true);

        // Act
        categoryService.deleteCategory(1L);

        // Assert
        verify(categoryRepository, times(1)).existsById(1L);
        verify(categoryRepository, times(1)).deleteById(1L);
    }

    /**
     * Test : Échouer lors de la suppression si la catégorie n'existe pas.
     * 
     * Conditions :
     * - La catégorie n'existe pas
     * 
     * Résultat attendu :
     * - Une IllegalArgumentException est levée
     */
    @Test
    @DisplayName("Doit lever une exception si la catégorie n'existe pas lors de la suppression")
    void testDeleteCategory_NotFound() {
        // Arrange
        when(categoryRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.deleteCategory(999L));

        assertTrue(exception.getMessage().contains("n'existe pas"));
        verify(categoryRepository, never()).deleteById(any());
    }

    /**
     * Test : Récupérer toutes les catégories actives du système.
     * 
     * Conditions :
     * - 3 catégories actives existent
     * 
     * Résultat attendu :
     * - Les 3 catégories sont retournées
     */
    @Test
    @DisplayName("Doit récupérer toutes les catégories actives du système")
    void testGetAllActiveCategories() {
        // Arrange
        Category category2 = Category.builder()
                .id(2L)
                .name("Physique")
                .status("active")
                .categoryOwner("teacher-002")
                .createdAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findByStatus("active"))
                .thenReturn(Arrays.asList(mockCategory, category2));

        // Act
        List<CategoryResponseDTO> results = categoryService.getAllActiveCategories();

        // Assert
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(c -> "active".equals(c.getStatus())));
        verify(categoryRepository, times(1)).findByStatus("active");
    }
}
