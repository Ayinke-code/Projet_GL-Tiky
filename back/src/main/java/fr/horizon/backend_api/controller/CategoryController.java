package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.CategoryResponseDTO;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.CreateCategoryDTO;
import fr.horizon.backend_api.exception.CategoryAlreadyExistsException;
import fr.horizon.backend_api.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestion des catégories.
 * Fournit les endpoints pour créer, récupérer, mettre à jour et supprimer les
 * catégories.
 * 
 * @author Tiky Team
 * @version 1.0
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*", maxAge = 3600)
public class CategoryController {

    /**
     * Service pour les opérations métier sur les catégories.
     */
    private final CategoryService categoryService;

    /**
     * Crée une nouvelle catégorie.
     * 
     * Endpoint: POST /api/categories
     * 
     * @param dto       Les données de création de la catégorie
     * @param teacherId L'identifiant de l'enseignant (en header ou paramètre)
     * @return La catégorie créée avec le code HTTP 201
     * @throws CategoryAlreadyExistsException Si une catégorie avec le même nom
     *                                        existe déjà
     */
    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @Valid @RequestBody CreateCategoryDTO dto,
            @RequestHeader(value = "X-Teacher-ID", defaultValue = "teacher-default") String teacherId) {

        CategoryResponseDTO createdCategory = categoryService.createCategory(dto, teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    /**
     * Récupère toutes les catégories d'un enseignant.
     * 
     * Endpoint: GET /api/categories/teacher
     * 
     * @param teacherId L'identifiant de l'enseignant
     * @return Liste des catégories de l'enseignant
     */
    @GetMapping("/teacher")
    public ResponseEntity<List<CategoryResponseDTO>> getCategoriesByTeacher(
            @RequestHeader(value = "X-Teacher-ID", defaultValue = "teacher-default") String teacherId) {
        List<CategoryResponseDTO> categories = categoryService.getAllActiveCategoriesForTeacher(teacherId);
        return ResponseEntity.ok(categories);
    }

    /**
     * Récupère toutes les catégories actives d'un enseignant.
     * 
     * Endpoint: GET /api/categories/teacher/active
     * 
     * @param teacherId L'identifiant de l'enseignant
     * @return Liste des catégories actives
     */
    @GetMapping("/teacher/active")
    public ResponseEntity<List<CategoryResponseDTO>> getActiveCategoriesByTeacher(
            @RequestHeader(value = "X-Teacher-ID", defaultValue = "teacher-default") String teacherId) {
        List<CategoryResponseDTO> categories = categoryService.getActiveCategoriesByTeacher(teacherId);
        return ResponseEntity.ok(categories);
    }

    /**
     * Récupère toutes les catégories actives du système.
     * 
     * Endpoint: GET /api/categories/active
     * 
     * @return Liste de toutes les catégories actives
     */
    @GetMapping("/active")
    public ResponseEntity<List<CategoryResponseDTO>> getAllActiveCategories() {
        List<CategoryResponseDTO> categories = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Récupère une catégorie par son identifiant.
     * 
     * Endpoint: GET /api/categories/{id}
     * 
     * @param id L'identifiant de la catégorie
     * @return La catégorie trouvée ou une réponse 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère une catégorie par son nom.
     * 
     * Endpoint: GET /api/categories/search?name={name}
     * 
     * @param name Le nom de la catégorie
     * @return La catégorie trouvée ou une réponse 404
     */
    @GetMapping("/search")
    public ResponseEntity<CategoryResponseDTO> getCategoryByName(@RequestParam String name) {
        return categoryService.getCategoryByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Met à jour une catégorie existante.
     * 
     * Endpoint: PUT /api/categories/{id}
     * 
     * @param id  L'identifiant de la catégorie
     * @param dto Les nouvelles données
     * @return La catégorie mise à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateCategoryDTO dto,
            @RequestHeader(value = "X-Teacher-ID", defaultValue = "teacher-default") String requesterId) {
 
        CategoryResponseDTO updatedCategory = categoryService.updateCategory(id, dto, requesterId);
        return ResponseEntity.ok(updatedCategory);
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<CategoryResponseDTO> archiveCategory(
            @PathVariable Long id,
            @RequestHeader(value = "X-Teacher-ID", defaultValue = "teacher-default") String requesterId) {
        return ResponseEntity.ok(categoryService.archiveCategory(id, requesterId));
    }
 
    @PatchMapping("/{id}/unarchive")
    public ResponseEntity<CategoryResponseDTO> unarchiveCategory(
            @PathVariable Long id,
            @RequestHeader(value = "X-Teacher-ID", defaultValue = "teacher-default") String requesterId) {
        return ResponseEntity.ok(categoryService.unarchiveCategory(id, requesterId));
    }
 
    /**
     * Supprime une catégorie.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @RequestHeader(value = "X-Teacher-ID", defaultValue = "teacher-default") String requesterId) {
        categoryService.deleteCategory(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gestionnaire global pour les exceptions.
     * 
     * @param e L'exception levée
     * @return Une réponse d'erreur avec le code HTTP approprié
     */
    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleCategoryAlreadyExists(CategoryAlreadyExistsException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "CATEGORY_ALREADY_EXISTS");
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<Void> followCategory(
            @PathVariable Long id,
            @RequestHeader(value = "X-Teacher-ID", defaultValue = "teacher-default") String teacherId) {
        categoryService.followCategory(id, teacherId);
        return ResponseEntity.ok().build();
    }
 
    @DeleteMapping("/{id}/follow")
    public ResponseEntity<Void> unfollowCategory(
            @PathVariable Long id,
            @RequestHeader(value = "X-Teacher-ID", defaultValue = "teacher-default") String teacherId) {
        categoryService.unfollowCategory(id, teacherId);
        return ResponseEntity.ok().build();
    }
 
    @GetMapping("/followed")
    public ResponseEntity<List<Long>> getFollowedCategoryIds(
            @RequestHeader(value = "X-Teacher-ID", defaultValue = "teacher-default") String teacherId) {
        return ResponseEntity.ok(categoryService.getFollowedCategoryIds(teacherId));
    }
 
    @GetMapping("/{id}/followers")
    public ResponseEntity<List<User>> getFollowers(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getFollowers(id));
    }

    @PostMapping("/{id}/followers")
    public ResponseEntity<Void> addFollower(@PathVariable Long id, @RequestParam String userId) {
        categoryService.addFollower(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/followers/{userId}")
    public ResponseEntity<Void> removeFollower(@PathVariable Long id, @PathVariable String userId) {
        categoryService.removeFollower(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/available-teachers")
    public ResponseEntity<List<User>> getAvailableTeachers(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getAvailableTeachers(id));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "ILLEGAL_STATE");
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gestionnaire global pour les exceptions d'argument invalide.
     * 
     * @param e L'exception levée
     * @return Une réponse d'erreur avec le code HTTP 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "INVALID_ARGUMENT");
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
