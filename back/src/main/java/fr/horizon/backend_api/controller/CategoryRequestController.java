package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.CategoryRequestCreateDto;
import fr.horizon.backend_api.model.dto.CategoryRequestDto;
import fr.horizon.backend_api.service.CategoryRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion des demandes de création de catégories.
 *
 * @author Tiky Team
 * @version 1.0
 */
@RestController
@RequestMapping("/category-requests")
@RequiredArgsConstructor
public class CategoryRequestController {

    /**
     * Service métier des demandes de catégories.
     */
    private final CategoryRequestService categoryRequestService;

    /**
     * Récupère toutes les demandes.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<CategoryRequestDto> getAll() {
        return categoryRequestService.findAll();
    }

    /**
     * Crée une demande de catégorie.
     */
    @PreAuthorize("hasAnyAuthority('STUDENT')")
    @PostMapping
    public CategoryRequestDto create(@RequestBody CategoryRequestCreateDto dto) {
        return categoryRequestService.create(dto);
    }

    /**
     * Supprime une demande.
     */
    @PreAuthorize("hasAnyAuthority('TEACHER', 'STUDENT')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        categoryRequestService.deleteById(id);
    }
}
