package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.CategoryResponseDTO;
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
 *
 * @author Tiky Team
 * @version 1.0
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @Valid @RequestBody CreateCategoryDTO dto,
            @RequestHeader(value = "X-Teacher-ID", defaultValue = "teacher-default") String teacherId) {
        CategoryResponseDTO createdCategory = categoryService.createCategory(dto, teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    @GetMapping("/teacher")
    public ResponseEntity<List<CategoryResponseDTO>> getCategoriesByTeacher(
            @RequestHeader(value = "X-Teacher-ID", defaultValue = "teacher-default") String teacherId) {
        List<CategoryResponseDTO> categories = categoryService.getAllActiveCategoriesForTeacher(teacherId);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/teacher/active")
    public ResponseEntity<List<CategoryResponseDTO>> getActiveCategoriesByTeacher(
            @RequestHeader(value = "X-Teacher-ID", defaultValue = "teacher-default") String teacherId) {
        List<CategoryResponseDTO> categories = categoryService.getActiveCategoriesByTeacher(teacherId);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/active")
    public ResponseEntity<List<CategoryResponseDTO>> getAllActiveCategories() {
        List<CategoryResponseDTO> categories = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<CategoryResponseDTO> getCategoryByName(@RequestParam String name) {
        return categoryService.getCategoryByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @RequestHeader(value = "X-Teacher-ID", defaultValue = "teacher-default") String requesterId) {
        categoryService.deleteCategory(id, requesterId);
        return ResponseEntity.noContent().build();
    }

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
}
