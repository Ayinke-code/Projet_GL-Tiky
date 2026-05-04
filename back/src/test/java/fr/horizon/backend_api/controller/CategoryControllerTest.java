package fr.horizon.backend_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.horizon.backend_api.model.dto.CategoryResponseDTO;
import fr.horizon.backend_api.model.dto.CreateCategoryDTO;
import fr.horizon.backend_api.exception.CategoryAlreadyExistsException;
import fr.horizon.backend_api.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour le contrôleur CategoryController.
 * Teste les endpoints REST pour les catégories.
 * 
 * @author Tiky Team
 * @version 1.0
 */
@WebMvcTest(CategoryController.class)
@DisplayName("Tests unitaires - CategoryController")
class CategoryControllerTest {

    /**
     * Mock MVC pour tester les endpoints REST.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Mock du service des catégories.
     */
    @MockitoBean
    private CategoryService categoryService;

    /**
     * ObjectMapper pour convertir les objets en JSON.
     */
    @Autowired
    private ObjectMapper objectMapper;

    private CategoryResponseDTO mockCategoryDTO;
    private CreateCategoryDTO createCategoryDTO;
    private String teacherId = "teacher-001";

    /**
     * Configuration avant chaque test.
     */
    @BeforeEach
    void setUp() {
        mockCategoryDTO = CategoryResponseDTO.builder()
                .id(1L)
                .name("Mathématiques")
                .status("active")
                .owner(teacherId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createCategoryDTO = CreateCategoryDTO.builder()
                .name("Mathématiques")
                .build();
    }

    /**
     * Test : POST /api/categories - Créer une catégorie avec succès.
     * 
     * Conditions :
     * - Le DTO de création est valide
     * 
     * Résultat attendu :
     * - Le code HTTP 201 (CREATED) est retourné
     * - La catégorie est retournée en JSON
     */
    @Test
    @DisplayName("Doit créer une catégorie avec le code HTTP 201")
    void testCreateCategory_Success() throws Exception {
        // Arrange
        when(categoryService.createCategory(any(CreateCategoryDTO.class), anyString()))
                .thenReturn(mockCategoryDTO);

        // Act & Assert
        ResultActions response = mockMvc.perform(post("/api/categories")
                .header("X-Teacher-ID", teacherId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createCategoryDTO)));

        response
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Mathématiques")))
                .andExpect(jsonPath("$.status", is("active")))
                .andExpect(jsonPath("$.owner", is(teacherId)));

        verify(categoryService, times(1)).createCategory(any(), anyString());
    }

    /**
     * Test : POST /api/categories - Échouer si le nom existe déjà.
     * 
     * Conditions :
     * - Une catégorie avec le même nom existe déjà
     * 
     * Résultat attendu :
     * - Le code HTTP 409 (CONFLICT) est retourné
     */
    @Test
    @DisplayName("Doit retourner le code HTTP 409 si la catégorie existe déjà")
    void testCreateCategory_AlreadyExists() throws Exception {
        // Arrange
        when(categoryService.createCategory(any(CreateCategoryDTO.class), anyString()))
                .thenThrow(new CategoryAlreadyExistsException("Une catégorie avec ce nom existe déjà"));

        // Act & Assert
        mockMvc.perform(post("/api/categories")
                .header("X-Teacher-ID", teacherId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createCategoryDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("CATEGORY_ALREADY_EXISTS")));

        verify(categoryService, times(1)).createCategory(any(), anyString());
    }

    /**
     * Test : POST /api/categories - Échouer si le nom est vide.
     * 
     * Conditions :
     * - Le nom du DTO est vide
     * 
     * Résultat attendu :
     * - Le code HTTP 400 (BAD_REQUEST) est retourné
     */
    @Test
    @DisplayName("Doit retourner le code HTTP 400 si le nom est vide")
    void testCreateCategory_NomVide() throws Exception {
        // Arrange
        CreateCategoryDTO emptyDto = CreateCategoryDTO.builder()
                .name("")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/categories")
                .header("X-Teacher-ID", teacherId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyDto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test : GET /api/categories/teacher - Récupérer toutes les catégories d'un
     * enseignant.
     * 
     * Conditions :
     * - L'enseignant a 2 catégories
     * 
     * Résultat attendu :
     * - Le code HTTP 200 (OK) est retourné
     * - Un tableau de 2 catégories est retourné
     */
    @Test
    @DisplayName("Doit récupérer toutes les catégories d'un enseignant")
    void testGetCategoriesByTeacher() throws Exception {
        // Arrange
        CategoryResponseDTO category2 = CategoryResponseDTO.builder()
                .id(2L)
                .name("Physique")
                .status("active")
                .owner(teacherId)
                .createdAt(LocalDateTime.now())
                .build();

        when(categoryService.getCategoriesByTeacher(teacherId))
                .thenReturn(Arrays.asList(mockCategoryDTO, category2));

        // Act & Assert
        mockMvc.perform(get("/api/categories/teacher")
                .header("X-Teacher-ID", teacherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(2)))
                .andExpect(jsonPath("$[0].name", is("Mathématiques")))
                .andExpect(jsonPath("$[1].name", is("Physique")));

        verify(categoryService, times(1)).getCategoriesByTeacher(teacherId);
    }

    /**
     * Test : GET /api/categories/teacher/active - Récupérer les catégories actives
     * d'un enseignant.
     * 
     * Conditions :
     * - L'enseignant a 1 catégorie active
     * 
     * Résultat attendu :
     * - Le code HTTP 200 (OK) est retourné
     * - Un tableau d'1 catégorie est retourné
     */
    @Test
    @DisplayName("Doit récupérer les catégories actives d'un enseignant")
    void testGetActiveCategoriesByTeacher() throws Exception {
        // Arrange
        when(categoryService.getActiveCategoriesByTeacher(teacherId))
                .thenReturn(Arrays.asList(mockCategoryDTO));

        // Act & Assert
        mockMvc.perform(get("/api/categories/teacher/active")
                .header("X-Teacher-ID", teacherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(1)))
                .andExpect(jsonPath("$[0].status", is("active")));

        verify(categoryService, times(1)).getActiveCategoriesByTeacher(teacherId);
    }

    /**
     * Test : GET /api/categories/active - Récupérer toutes les catégories actives
     * du système.
     * 
     * Conditions :
     * - 2 catégories actives existent
     * 
     * Résultat attendu :
     * - Le code HTTP 200 (OK) est retourné
     * - Un tableau de 2 catégories est retourné
     */
    @Test
    @DisplayName("Doit récupérer toutes les catégories actives du système")
    void testGetAllActiveCategories() throws Exception {
        // Arrange
        when(categoryService.getAllActiveCategories())
                .thenReturn(Arrays.asList(mockCategoryDTO));

        // Act & Assert
        mockMvc.perform(get("/api/categories/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(1)))
                .andExpect(jsonPath("$[0].status", is("active")));

        verify(categoryService, times(1)).getAllActiveCategories();
    }

    /**
     * Test : GET /api/categories/{id} - Récupérer une catégorie par son ID.
     * 
     * Conditions :
     * - La catégorie existe
     * 
     * Résultat attendu :
     * - Le code HTTP 200 (OK) est retourné
     * - La catégorie est retournée
     */
    @Test
    @DisplayName("Doit récupérer une catégorie par son ID")
    void testGetCategoryById() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(1L))
                .thenReturn(Optional.of(mockCategoryDTO));

        // Act & Assert
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Mathématiques")));

        verify(categoryService, times(1)).getCategoryById(1L);
    }

    /**
     * Test : GET /api/categories/{id} - Retourner 404 si la catégorie n'existe pas.
     * 
     * Conditions :
     * - La catégorie n'existe pas
     * 
     * Résultat attendu :
     * - Le code HTTP 404 (NOT_FOUND) est retourné
     */
    @Test
    @DisplayName("Doit retourner le code HTTP 404 si la catégorie n'existe pas")
    void testGetCategoryById_NotFound() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/categories/999"))
                .andExpect(status().isNotFound());

        verify(categoryService, times(1)).getCategoryById(999L);
    }

    /**
     * Test : GET /api/categories/search?name={name} - Récupérer une catégorie par
     * son nom.
     * 
     * Conditions :
     * - La catégorie existe
     * 
     * Résultat attendu :
     * - Le code HTTP 200 (OK) est retourné
     * - La catégorie est retournée
     */
    @Test
    @DisplayName("Doit récupérer une catégorie par son nom")
    void testGetCategoryByName() throws Exception {
        // Arrange
        when(categoryService.getCategoryByName("Mathématiques"))
                .thenReturn(Optional.of(mockCategoryDTO));

        // Act & Assert
        mockMvc.perform(get("/api/categories/search?name=Mathématiques"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Mathématiques")));

        verify(categoryService, times(1)).getCategoryByName("Mathématiques");
    }

    /**
     * Test : DELETE /api/categories/{id} - Supprimer une catégorie.
     * 
     * Conditions :
     * - La catégorie existe
     * 
     * Résultat attendu :
     * - Le code HTTP 204 (NO_CONTENT) est retourné
     */
    @Test
    @DisplayName("Doit supprimer une catégorie avec le code HTTP 204")
    void testDeleteCategory() throws Exception {
        // Arrange
        doNothing().when(categoryService).deleteCategory(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService, times(1)).deleteCategory(1L);
    }

    /**
     * Test : DELETE /api/categories/{id} - Échouer si la catégorie n'existe pas.
     * 
     * Conditions :
     * - La catégorie n'existe pas
     * 
     * Résultat attendu :
     * - Le code HTTP 400 (BAD_REQUEST) est retourné
     */
    @Test
    @DisplayName("Doit retourner le code HTTP 400 si la catégorie n'existe pas lors de la suppression")
    void testDeleteCategory_NotFound() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("La catégorie n'existe pas"))
                .when(categoryService).deleteCategory(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/categories/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_ARGUMENT")));

        verify(categoryService, times(1)).deleteCategory(999L);
    }
}
