package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.CategoryDto;
import fr.horizon.backend_api.model.dto.CategoryResponseDto;
import fr.horizon.backend_api.exception.CategoryAlreadyExistsException;
import fr.horizon.backend_api.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestion complète des catégories.
 * Fusionne la sécurité avancée et les fonctionnalités de suivi
 * (Following/Approval).
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    // =========================
    // LECTURE / RECHERCHE
    // =========================

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/active")
    public ResponseEntity<List<CategoryResponseDto>> getAllActiveCategories() {
        return ResponseEntity.ok(categoryService.getAllActiveCategories());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'TEACHER')")
    @GetMapping("/teacher")
    public ResponseEntity<List<CategoryResponseDto>> getCategoriesByTeacher(
            @RequestHeader(value = "X-Teacher-ID") String teacherId) {
        return ResponseEntity.ok(categoryService.getAllActiveCategoriesForTeacher(teacherId));
    }

    // =========================
    // CRÉATION / MODIFICATION
    // =========================

    @PreAuthorize("hasAnyAuthority('ADMIN', 'TEACHER')")
    @PostMapping
    public ResponseEntity<CategoryResponseDto> createCategory(
            @Valid @RequestBody CategoryRequestDto dto,
            @RequestHeader(value = "X-Teacher-ID") String teacherId) {
        CategoryResponseDto created = categoryService.createCategory(dto, teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDto dto,
            @RequestHeader(value = "X-Teacher-ID") String requesterId) {
        return ResponseEntity.ok(categoryService.updateCategory(id, dto, requesterId));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @RequestHeader(value = "X-Teacher-ID") String requesterId) {
        categoryService.deleteCategory(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    // =========================
    // ARCHIVAGE
    // =========================

    @PreAuthorize("hasAnyAuthority('ADMIN', 'TEACHER')")
    @PatchMapping("/{id}/archive")
    public ResponseEntity<CategoryResponseDto> archiveCategory(
            @PathVariable Long id,
            @RequestHeader(value = "X-Teacher-ID") String requesterId) {
        return ResponseEntity.ok(categoryService.archiveCategory(id, requesterId));
    }

    // =========================
    // SYSTÈME DE FOLLOWERS (ENSEIGNANTS)
    // =========================

    @PreAuthorize("hasAuthority('TEACHER')")
    @PostMapping("/{id}/follow")
    public ResponseEntity<Void> followCategory(
            @PathVariable Long id,
            @RequestHeader(value = "X-Teacher-ID") String teacherId) {
        categoryService.followCategory(id, teacherId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'TEACHER')")
    @PostMapping("/{id}/approve/{teacherId}")
    public ResponseEntity<Void> approveFollower(
            @PathVariable Long id,
            @PathVariable String teacherId,
            @RequestHeader(value = "X-Teacher-ID") String requesterId) {
        categoryService.approveFollower(id, teacherId, requesterId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/available-teachers")
    public ResponseEntity<List<User>> getAvailableTeachers(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getAvailableTeachers(id));
    }

    // =========================
    // GESTION DES ERREURS
    // =========================

    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleCategoryAlreadyExists(CategoryAlreadyExistsException e) {
        return createErrorResponse("CATEGORY_ALREADY_EXISTS", e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException e) {
        return createErrorResponse("BAD_REQUEST", e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurity(SecurityException e) {
        return createErrorResponse("FORBIDDEN", e.getMessage(), HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<Map<String, String>> createErrorResponse(String code, String message, HttpStatus status) {
        Map<String, String> error = new HashMap<>();
        error.put("error", code);
        error.put("message", message);
        return ResponseEntity.status(status).body(error);
    }
}