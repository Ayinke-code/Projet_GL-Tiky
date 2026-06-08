package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.HandleLinkRequestDto;
import fr.horizon.backend_api.model.dto.OfficialLinkRequestDto;
import fr.horizon.backend_api.model.dto.RelationDto;
import fr.horizon.backend_api.model.dto.RelationRequestDto;
import fr.horizon.backend_api.service.IssueFollowerService;
import fr.horizon.backend_api.service.IssueRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur des relations entre tickets.
 * Expose les endpoints REST permettant d'associer et de gérer les dépendances.
 *
 * @author Tiky Team
 * @version 1.1
 */
@RestController
@RequestMapping("/issueRelations")
@RequiredArgsConstructor
public class IssueRelationController {

    private final IssueRelationService relationService;
    private final IssueFollowerService issueFollowerService;

    /**
     * Crée une nouvelle relation pour un ticket spécifique.
     *
     * @param id             identifiant du ticket source
     * @param dto            données de la relation à ajouter
     * @param authentication contexte d'authentification Spring Security
     * @return si le lien est créé, 202 ACCEPTED si une requête directe a été envoyée à un autre manager
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/relations")
    public ResponseEntity<RelationDto> linkIssue(
            @PathVariable Long id,
            @RequestBody RelationRequestDto dto,
            Authentication authentication) {

        User user = issueFollowerService.getUserByEmail(authentication.getName());
        RelationDto result = relationService.createRelation(id, dto, user);

        if (result == null) {
            return ResponseEntity.accepted().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Récupère toutes les relations associées à un ticket.
     *
     * @param id identifiant du ticket
     * @return liste des DTOs de relations
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/relations")
    public ResponseEntity<List<RelationDto>> getIssueRelations(@PathVariable Long id) {
        return ResponseEntity.ok(relationService.getRelationsByIssueId(id));
    }

    /**
     * Supprime une liaison existante entre deux tickets.
     *
     * @param relationId identifiant unique de la relation à supprimer
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/relations/{relationId}")
    public ResponseEntity<Void> unlinkIssue(@PathVariable Long relationId, Authentication authentication) {
        User user = issueFollowerService.getUserByEmail(authentication.getName());
        relationService.deleteRelation(relationId, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère toutes les demandes de liens officiels en attente pour le manager connecté.
     *
     * @param authentication contexte d'authentification Spring Security
     * @return Liste des demandes en attente
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/relations/requests")
    public ResponseEntity<List<OfficialLinkRequestDto>> getPendingRequests(Authentication authentication) {
        User user = issueFollowerService.getUserByEmail(authentication.getName());
        List<OfficialLinkRequestDto> requests = relationService.getPendingOfficialLinkRequests(user);
        return ResponseEntity.ok(requests);
    }

    /**
     * Permet à un manager de traiter (accepter ou refuser) une demande de lien officiel.
     *
     * @param requestId      identifiant de la demande (OfficialLinkRequest)
     * @param dto            décision d'approbation et motif éventuel de refus
     * @param authentication contexte d'authentification Spring Security
     * @return 201 CREATED si le lien officiel a été finalisé, 200 OK si refusé ou en attente de l'autre manager
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/relations/requests/{requestId}/handle")
    public ResponseEntity<RelationDto> handleLinkRequest(
            @PathVariable Long requestId,
            @RequestBody HandleLinkRequestDto dto,
            Authentication authentication) {

        User user = issueFollowerService.getUserByEmail(authentication.getName());
        RelationDto result = relationService.handleOfficialLinkRequest(requestId, dto, user);

        if (result == null) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}