package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.RequestManagerDto;
import fr.horizon.backend_api.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;


    /**
     * Endpoint pour créer une demande d'assignation de manager.
     * URL : POST /assign-manager/issue/12/target-user/5
     */
    @PostMapping("assign-manager/issue/{issueId}/target-user/{userId}")
    public ResponseEntity<RequestManagerDto> assignManager(
            @PathVariable Long issueId,
            @PathVariable Long userId) {

        RequestManagerDto dto = issueService.assignManagerToIssue(issueId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Endpoint pour accepter ou refuser une demande.
     * URL : PUT /demandes-manager/42/respond?accept=true&userId=5
     */
    @PutMapping("demandes-manager/{demandeId}/respond")
    public ResponseEntity<RequestManagerDto> respondToRequest(
            @PathVariable Long demandeId,
            @RequestParam boolean accept,
            // on devrait plutard remplacer par le jeton d'authentification
            @RequestParam Long userId) {

        RequestManagerDto dto = issueService.respondManagerRequest(accept, demandeId, userId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Récupère les demandes d'assignation en attente pour un enseignant spécifique.
     * URL : GET issues/demandes-manager/teacher?userId=5
     */
    @GetMapping("demandes-manager/teacher")
    public ResponseEntity<List<RequestManagerDto>> getTeacherAssignations(@RequestParam Long userId) {
        List<RequestManagerDto> list = issueService.getTeacherAssignations(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * Récupère les alertes pour un propriétaire de catégorie (tickets sans manager assigné).
     * URL : GET issues/demandes-manager/owner-alerts?userId=1
     */
    @GetMapping("demandes-manager/owner-alerts")
    public ResponseEntity<List<RequestManagerDto>> alertCategoryOwner(@RequestParam Long userId) {
        List<RequestManagerDto> list = issueService.alertCategoryOwner(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * Récupère l'historique des demandes (Acceptées, Refusées, ou En attente avec cible)
     * URL : GET /issues/demandes-manager/history?userId=1
     */
    @GetMapping("/demandes-manager/history")
    public ResponseEntity<List<RequestManagerDto>> getHistory(@RequestParam Long userId) {
        List<RequestManagerDto> list = issueService.getHistoricalRequestManager(userId);
        return ResponseEntity.ok(list);
    }
}