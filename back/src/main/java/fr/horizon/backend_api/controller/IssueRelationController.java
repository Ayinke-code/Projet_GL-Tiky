package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.RelationDto;
import fr.horizon.backend_api.model.dto.RelationRequestDto;
import fr.horizon.backend_api.service.IssueFollowerService;
import fr.horizon.backend_api.service.IssueRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur des relations entre tickets.
 * Expose les endpoints REST permettant d'associer et de gérer les dépendances.
 * Seuls les abonnés d'un ticket peuvent créer une liaison.
 *
 * @author Tiky Team
 * @version 1.0
 */
@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
public class IssueRelationController {

    private final IssueRelationService relationService;
    private final IssueFollowerService issueFollowerService;

    /**
     * Crée une nouvelle relation pour un ticket spécifique.
     * L'utilisateur authentifié doit suivre le ticket source.
     *
     * @param id             identifiant du ticket source
     * @param dto            données de la relation à ajouter
     * @param authentication contexte d'authentification Spring Security
     * @return le DTO de la relation créée
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/relations")
    public RelationDto linkIssue(
            @PathVariable Long id,
            @RequestBody RelationRequestDto dto,
            Authentication authentication) {
        User user = issueFollowerService.getUserByEmail(authentication.getName());
        return relationService.createRelation(id, dto, user);
    }

    /**
     * Récupère toutes les relations associées à un ticket (rendu bidirectionnel).
     *
     * @param id identifiant du ticket
     * @return liste des DTO de relations trouvées
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/relations")
    public List<RelationDto> getIssueRelations(@PathVariable Long id) {
        return relationService.getRelationsByIssueId(id);
    }

    /**
     * Supprime une liaison existante entre deux tickets.
     *
     * @param relationId identifiant unique de la relation à supprimer
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/relations/{relationId}")
    public void unlinkIssue(@PathVariable Long relationId) {
        relationService.deleteRelation(relationId);
    }
}