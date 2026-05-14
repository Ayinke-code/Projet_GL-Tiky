package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.CategoryRequestCreateDto;
import fr.horizon.backend_api.model.dto.CategoryRequestDto;
import fr.horizon.backend_api.service.CategoryRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST de gestion des demandes de création de catégories.
 *
 * Permet :
 * - de consulter les demandes existantes,
 * - de créer une nouvelle demande,
 * - de supprimer une demande,
 * - de valider une demande afin de créer automatiquement une catégorie.
 *
 * Les accès sont sécurisés selon les rôles utilisateur.
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
     * Récupère l'ensemble des demandes de catégories.
     *
     * Accessible à tout utilisateur authentifié.
     *
     * @return liste des demandes de catégories
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<CategoryRequestDto> getAll() {
        return categoryRequestService.findAll();
    }

    /**
     * Crée une nouvelle demande de catégorie.
     *
     * Accessible uniquement aux étudiants.
     *
     * @param dto données de création de la demande
     * @return demande créée
     */
    @PreAuthorize("hasAnyAuthority('STUDENT')")
    @PostMapping
    public CategoryRequestDto create(@RequestBody CategoryRequestCreateDto dto) {
        return categoryRequestService.create(dto);
    }

    /**
     * Supprime une demande de catégorie.
     *
     * Accessible aux enseignants et à l'auteur de la demande.
     *
     * @param id identifiant de la demande à supprimer
     */
    @PreAuthorize("hasAnyAuthority('TEACHER', 'STUDENT')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        categoryRequestService.deleteById(id);
    }

    /**
     * Valide une demande de catégorie.
     *
     * Cette action :
     * - crée automatiquement une catégorie,
     * - associe l'enseignant validateur,
     * - met à jour le statut de la demande.
     *
     * Accessible uniquement aux enseignants.
     *
     * @param id identifiant de la demande à valider
     * @param authentication utilisateur authentifié effectuant la validation
     */
    @PreAuthorize("hasAnyAuthority('TEACHER')")
    @PostMapping("/{id}/accept")
    public void accept(@PathVariable Long id, Authentication authentication) {
        categoryRequestService.accept(id, authentication.getName());
    }
}