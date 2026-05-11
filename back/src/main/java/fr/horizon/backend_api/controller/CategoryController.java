package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.CategoryDto;
import fr.horizon.backend_api.model.dto.CategoryCreateDto;
import fr.horizon.backend_api.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
public class CategoryController {

    /**
     * Service pour les opérations métier sur les catégories.
     */
    private final CategoryService categoryService;

    /**
     * Récupère l'ensemble des catégories.
     *
     * @return liste des catégories sous forme de DTO
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<CategoryDto> getAllCategories() {
        return categoryService.findAll();
    }

    /**
     * Crée une nouvelle catégorie.
     *
     * @param dto données de création de la catégorie
     * @return catégorie créée sous forme de DTO
     */
    @PreAuthorize("hasAnyAuthority('ADMIN','TEACHER')")
    @PostMapping
    public CategoryDto createCategory(@RequestBody CategoryCreateDto dto) {
        return categoryService.createCategory(dto);
    }

    /**
     * Supprime une catégorie à partir de son identifiant.
     *
     * @param id identifiant de la catégorie
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id) {
        categoryService.deleteById(id);
    }
}
