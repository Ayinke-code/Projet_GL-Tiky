package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.RequestManagerDTO;
import fr.horizon.backend_api.model.dto.IssueDTO;
import fr.horizon.backend_api.model.dto.UpdateIssueStatusDTO;
import fr.horizon.backend_api.service.IssueService;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<Issue> createIssue(@RequestBody IssueDTO dto, Authentication auth) {
        // Récupérer l'email depuis le token JWT
        String email = auth.getName();

        // Chercher l'utilisateur en base de données
        User creator = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Issue issue = issueService.createIssue(dto, creator);
        return ResponseEntity.status(HttpStatus.CREATED).body(issue);
    }

    @GetMapping
    public ResponseEntity<List<Issue>> getAllIssues() {
        return ResponseEntity.ok(issueService.getAllIssues());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Issue> getIssueById(@PathVariable Long id) {
        return ResponseEntity.ok(issueService.getIssueById(id));
    }

    @GetMapping("/category/{categorieId}")
    public ResponseEntity<List<Issue>> getIssuesByCategory(@PathVariable Long categorieId) {
        return ResponseEntity.ok(issueService.getIssuesByCategory(categorieId));
    }

    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<List<Issue>> getIssuesByCreator(@PathVariable Long creatorId) {
        return ResponseEntity.ok(issueService.getIssuesByCreator(creatorId));
    }

    @GetMapping("/status/{statut}")
    public ResponseEntity<List<Issue>> getIssuesByStatus(@PathVariable String statut) {
        return ResponseEntity.ok(issueService.getIssuesByStatus(statut));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIssue(@PathVariable Long id) {
        issueService.deleteIssue(id);
        return ResponseEntity.noContent().build();
    }


    /**
     * Endpoint pour créer une demande d'assignation de manager.
     * URL : POST /assign-manager/issue/12/target-user/5
     */
    @PostMapping("assign-manager/issue/{issueId}/target-user/{userId}")
    public ResponseEntity<RequestManagerDTO> assignManager(
            @PathVariable Long issueId,
            @PathVariable Long userId) {

        RequestManagerDTO dto = issueService.assignManagerToIssue(issueId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Endpoint pour accepter ou refuser une demande.
     * URL : PUT /demandes-manager/42/respond?accept=true&userId=5
     */
    @PutMapping("demandes-manager/{demandeId}/respond")
    public ResponseEntity<RequestManagerDTO> respondToRequest(
            @PathVariable Long demandeId,
            @RequestParam boolean accept,
            // on devrait plutard remplacer par le jeton d'authentification
            @RequestParam Long userId) {

        RequestManagerDTO dto = issueService.respondManagerRequest(accept, demandeId, userId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Récupère les demandes d'assignation en attente pour un enseignant spécifique.
     * URL : GET issues/demandes-manager/teacher?userId=5
     */
    @GetMapping("demandes-manager/teacher")
    public ResponseEntity<List<RequestManagerDTO>> getTeacherAssignations(@RequestParam Long userId) {
        List<RequestManagerDTO> list = issueService.getTeacherAssignations(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * Récupère les alertes pour un propriétaire de catégorie (tickets sans manager assigné).
     * URL : GET issues/demandes-manager/owner-alerts?userId=1
     */
    @GetMapping("demandes-manager/owner-alerts")
    public ResponseEntity<List<RequestManagerDTO>> alertCategoryOwner(@RequestParam Long userId) {
        List<RequestManagerDTO> list = issueService.alertCategoryOwner(userId);
        return ResponseEntity.ok(list);
    }



    @PutMapping("/{id}/status")
    public ResponseEntity<Issue> updateIssueStatus(@PathVariable Long id, @RequestBody UpdateIssueStatusDTO dto) {
        Issue updatedIssue = issueService.updateIssueStatus(id, dto.getStatut());
        return ResponseEntity.ok(updatedIssue);
    }

    /**
     * Récupère l'historique des demandes (Acceptées, Refusées, ou En attente avec cible)
     * URL : GET /issues/demandes-manager/history?userId=1
     */
    @GetMapping("/demandes-manager/history")
    public ResponseEntity<List<RequestManagerDTO>> getHistory(@RequestParam Long userId) {
        List<RequestManagerDTO> list = issueService.getHistoricalRequestManager(userId);
        return ResponseEntity.ok(list);
    }
}
