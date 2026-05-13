package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.dto.RequestManagerDTO;
import fr.horizon.backend_api.model.dto.IssueDTO;
import fr.horizon.backend_api.model.dto.IssueResponseDTO;
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
    public ResponseEntity<IssueResponseDTO> createIssue(@RequestBody IssueDTO dto, Authentication auth) {
        // Récupérer l'email depuis le token JWT
        String email = auth.getName();

        // Chercher l'utilisateur en base de données
        User creator = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (dto.getDescription() != null && dto.getDescription().length() > 4000) {
            throw new RuntimeException("La description dépasse la limite de 4000 caractères");
        }

        Issue issue = issueService.createIssue(dto, creator);
        IssueResponseDTO response = IssueResponseDTO.builder()
                .idTick(issue.getId())
                .titre(issue.getTitle())
                .description(issue.getDescription())
                .statut(issue.getStatus() != null ? issue.getStatus().toString() : "OUVERT")
                .estPrive(issue.isPrivate())
                .dateCreation(issue.getCreatedAt())
                .dateModification(issue.getUpdatedAt())
                .category(issue.getCategory() != null ? IssueResponseDTO.CategoryNameDTO.builder()
                        .id(issue.getCategory().getId())
                        .name(issue.getCategory().getName())
                        .nom(issue.getCategory().getName())
                        .subject(issue.getCategory().getSubject())
                        .build() : null)
                .createdBy(issue.getCreatedBy() != null ? IssueResponseDTO.UserNameDTO.builder()
                        .id(issue.getCreatedBy().getId())
                        .name(issue.getCreatedBy().getName())
                        .email(issue.getCreatedBy().getEmail())
                        .role(issue.getCreatedBy().getDisplayRole())
                        .roles(issue.getCreatedBy().getRoleNames())
                        .build() : null)
                .manager(issue.getManager() != null ? IssueResponseDTO.UserNameDTO.builder()
                        .id(issue.getManager().getId())
                        .name(issue.getManager().getName())
                        .email(issue.getManager().getEmail())
                        .role(issue.getManager().getDisplayRole())
                        .roles(issue.getManager().getRoleNames())
                        .build() : null)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<IssueResponseDTO>> getAllIssues() {
        return ResponseEntity.ok(issueService.getAllIssuesAsDTO());
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
     * URL : POST /demandes-manager/issue/12/target-user/5
     */
    @PostMapping("demandes-manager/issue/{issueId}/target-user/{userId}")
    public ResponseEntity<RequestManagerDTO> createRequest(
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






    @PutMapping("/{id}/status")
    public ResponseEntity<Issue> updateIssueStatus(@PathVariable Long id, @RequestBody UpdateIssueStatusDTO dto) {
        Issue updatedIssue = issueService.updateIssueStatus(id, dto.getStatut());
        return ResponseEntity.ok(updatedIssue);
    }
}
