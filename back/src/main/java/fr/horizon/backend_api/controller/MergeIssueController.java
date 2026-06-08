package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.MergeRequestDto;
import fr.horizon.backend_api.service.MergeIssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
Fusion des tickets
 */
@RestController
@RequestMapping("/merging")
@RequiredArgsConstructor
public class MergeIssueController {

    private final MergeIssueService mergeIssueService;

    /**
     * Initie une nouvelle demande de fusion entre deux tickets.
     *
     * @param id             Identifiant du ticket source (celui qui sera absorbé)
     * @param targetId       Identifiant du ticket cible (le ticket principal)
     * @param authentication Le contexte de sécurité (l'utilisateur courant)
     * @return Le DTO de la demande avec le code 201 CREATED
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/merge")
    public ResponseEntity<MergeRequestDto> requestMerge(
            @PathVariable Long id,
            @RequestParam Long targetId,
            Authentication authentication) {

        String email = authentication.getName();
        MergeRequestDto result = mergeIssueService.sendMergeRequest(id, targetId, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Permet à un manager de répondre (accepter ou refuser) à une demande de fusion.
     *
     * @param requestId      Identifiant de la demande de fusion (MergeRequest)
     * @param accept         Booléen indiquant si la fusion est acceptée
     * @param reason         Raison textuelle, obligatoire en cas de refus
     * @param authentication Le contexte de sécurité (le manager courant)
     * @return Le DTO mis à jour avec le code 200 OK
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{requestId}/respond")
    public ResponseEntity<MergeRequestDto> respondToMergeRequest(
            @PathVariable Long requestId,
            @RequestParam boolean accept,
            @RequestParam(required = false) String reason,
            Authentication authentication) {

        String email = authentication.getName();
        MergeRequestDto result = mergeIssueService.respondToMergeRequest(requestId, accept, reason, email);
        return ResponseEntity.ok(result);
    }

    /**
     * Récupère la liste de toutes les demandes de fusion en attente d'approbation
     * pour le manager actuellement connecté.
     *
     * @param authentication Le contexte de sécurité (le manager courant)
     * @return Liste des demandes en attente avec le code 200 OK
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/requests")
    public ResponseEntity<List<MergeRequestDto>> getPendingMergeRequests(Authentication authentication) {
        String email = authentication.getName();
        List<MergeRequestDto> requests = mergeIssueService.getManagerRequestDto(email);
        return ResponseEntity.ok(requests);
    }
}