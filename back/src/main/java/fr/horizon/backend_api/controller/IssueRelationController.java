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
     * Récupère toutes les relations associées à un ticket.
     * Le filtrage par rôle est géré côté front.
     *
     * @param id identifiant du ticket
     * @return liste des DTOs de relations
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/relations")
    public List<RelationDto> getIssueRelations(@PathVariable Long id) {
        return relationService.getRelationsByIssueId(id);
    }

    /**
     * Supprime une liaison existante entre deux tickets.
     *
     * @param relationId     identifiant unique de la relation à supprimer
     * @param authentication contexte d'authentification Spring Security
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/relations/{relationId}")
    public void unlinkIssue(
            @PathVariable Long relationId,
            Authentication authentication) {
        User user = issueFollowerService.getUserByEmail(authentication.getName());
        relationService.deleteRelation(relationId, user);
    }

    /**
     * Permet au second manager de valider ou refuser un lien en attente.
     *
     * @param relationId     identifiant de la relation à valider
     * @param accept         true pour valider, false pour refuser
     * @param authentication contexte d'authentification Spring Security
     * @return le DTO de la relation mise à jour
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/relations/{relationId}/validate")
    public RelationDto validateRelation(
            @PathVariable Long relationId,
            @RequestParam boolean accept,
            Authentication authentication) {
        User user = issueFollowerService.getUserByEmail(authentication.getName());
        return relationService.validateRelation(relationId, accept, user);
    }

    /**
     * Récupère tous les liens en attente de validation pour le manager connecté.
     *
     * @param authentication contexte d'authentification Spring Security
     * @return liste des DTOs de relations en attente
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/relations/pending")
    public List<RelationDto> getPendingRelations(Authentication authentication) {
        User user = issueFollowerService.getUserByEmail(authentication.getName());
        return relationService.getPendingRelationsForValidator(user);
    }
}