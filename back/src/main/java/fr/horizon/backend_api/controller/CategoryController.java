package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.CategoryDetailsDto;
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
     * Récupère une catégorie par son identifiant.
     *
     * @param id identifiant de la catégorie
     * @return détails de la catégorie
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public CategoryDetailsDto getCategorieById(@PathVariable Long id) {
        return categoryService.findById(id);
    }

    /**
     * Supprime un utilisateur des followers d'une catégorie.
     *
     * @param idUser identifiant de l'utilisateur
     * @param idCategory identifiant de la catégorie
     */
    @PreAuthorize("hasAnyAuthority('ADMIN','TEACHER')")
    @DeleteMapping("/followers/{idUser}/{idCategory}")
    public void deleteCategoryFollower(@PathVariable Long idUser, @PathVariable Long idCategory) {
        categoryService.deleteFollowerById(idCategory, idUser);
    }

    /**
     * Ajoute un utilisateur aux followers d'une catégorie.
     *
     * @param idUser identifiant de l'utilisateur
     * @param idCategory identifiant de la catégorie
     * @return catégorie mise à jour
     */
    @PreAuthorize("hasAnyAuthority('ADMIN','TEACHER')")
    @PostMapping("/followers/{idUser}/{idCategory}")
    public CategoryDetailsDto addCategoryFollower(@PathVariable Long idUser, @PathVariable Long idCategory) {
        categoryService.addFollower(idCategory, idUser);
        return categoryService.findById(idCategory);
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
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TEACHER')")
    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id) {
        categoryService.deleteById(id);
    }
}
