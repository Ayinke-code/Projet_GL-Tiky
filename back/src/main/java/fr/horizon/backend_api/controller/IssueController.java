package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.IssueFollower;
import fr.horizon.backend_api.model.enums.FollowedReason;
import fr.horizon.backend_api.model.dto.IssueDTO;
import fr.horizon.backend_api.model.dto.UpdateIssueStatusDTO;
import fr.horizon.backend_api.service.IssueService;
import fr.horizon.backend_api.service.IssueFollowerService;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;
    private final UserRepository userRepository;
    private final IssueFollowerService issueFollowerService;

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

    @PutMapping("/{id}/status")
    public ResponseEntity<Issue> updateIssueStatus(@PathVariable Long id, @RequestBody UpdateIssueStatusDTO dto) {
        Issue updatedIssue = issueService.updateIssueStatus(id, dto.getStatut());
        return ResponseEntity.ok(updatedIssue);
    }

    @PostMapping("/{id}/follow") // Suivre une issue
    public ResponseEntity<IssueFollower> followIssue(
            @PathVariable Long id,
            @RequestParam int raison,
            Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        IssueFollower follower = issueFollowerService.followIssue(id, raison, user);
        if (raison < 0 || raison > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Raison invalide. Valeurs acceptées : 0, 1, 2");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(follower);

    }

    @GetMapping("/followed") // Voir les issues suivies
    public ResponseEntity<List<Issue>> getFollowedIssues(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ResponseEntity.ok(issueFollowerService.getFollowedIssues(user));
    }
}
