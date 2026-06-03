package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.RelationDto;
import fr.horizon.backend_api.model.dto.RelationRequestDto;
import fr.horizon.backend_api.service.IssueRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Contrôleur des relations entre issues.
 * Expose les endpoints REST permettant d'associer et de gérer les dépendances.
 *
 * @author Tiky Team
 * @version 1.0
 */
@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
public class IssueRelationController {

    private final IssueRelationService relationService;

    /**
     * Crée une nouvelle relation pour une issue spécifique.
     *
     * @param id  identifiant de l'issue source
     * @param dto données de la relation à ajouter
     * @return le DTO de la relation créée
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/relations")
    public RelationDto linkIssue(@PathVariable Long id, @RequestBody RelationRequestDto dto) {
        return relationService.createRelation(id, dto);
    }

    /**
     * Récupère toutes les relations associées à une issue (Rendu bidirectionnel).
     *
     * @param id identifiant de l'issue
     * @return liste des DTO de relations trouvées
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/relations")
    public List<RelationDto> getIssueRelations(@PathVariable Long id) {
        return relationService.getRelationsByIssueId(id);
    }

    /**
     * Supprime une liaison existante entre deux issues.
     *
     * @param relationId identifiant unique de la relation à supprimer
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/relations/{relationId}")
    public void unlinkIssue(@PathVariable Long relationId) {
        relationService.deleteRelation(relationId);
    }
}