package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.IssueFollower;
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
import java.util.List;

/**
 * Contrôleur REST gérant les endpoints liés aux issues et aux abonnements.
 */
@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;
    private final UserRepository userRepository;
    private final IssueFollowerService issueFollowerService;

    /**
     * Crée une nouvelle issue.
     * @param dto données de l'issue
     * @param auth utilisateur authentifié
     * @return l'issue créée avec le statut 201
     */
    @PostMapping
    public ResponseEntity<Issue> createIssue(@RequestBody IssueDTO dto, Authentication auth) {
        String email = auth.getName();
        User creator = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Issue issue = issueService.createIssue(dto, creator);
        return ResponseEntity.status(HttpStatus.CREATED).body(issue);
    }

    /**
     * Récupère toutes les issues.
     * @return liste de toutes les issues
     */
    @GetMapping
    public ResponseEntity<List<Issue>> getAllIssues() {
        return ResponseEntity.ok(issueService.getAllIssues());
    }

    /**
     * Récupère une issue par son identifiant.
     * @param id identifiant de l'issue
     * @return l'issue correspondante
     */
    @GetMapping("/{id}")
    public ResponseEntity<Issue> getIssueById(@PathVariable Long id) {
        return ResponseEntity.ok(issueService.getIssueById(id));
    }

    /**
     * Récupère les issues d'une catégorie.
     * @param categorieId identifiant de la catégorie
     * @return liste des issues de la catégorie
     */
    @GetMapping("/category/{categorieId}")
    public ResponseEntity<List<Issue>> getIssuesByCategory(@PathVariable Long categorieId) {
        return ResponseEntity.ok(issueService.getIssuesByCategory(categorieId));
    }

    /**
     * Récupère les issues créées par un utilisateur.
     * @param creatorId identifiant du créateur
     * @return liste des issues du créateur
     */
    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<List<Issue>> getIssuesByCreator(@PathVariable Long creatorId) {
        return ResponseEntity.ok(issueService.getIssuesByCreator(creatorId));
    }

    /**
     * Récupère les issues par statut.
     * @param statut statut des issues
     * @return liste des issues avec ce statut
     */
    @GetMapping("/status/{statut}")
    public ResponseEntity<List<Issue>> getIssuesByStatus(@PathVariable String statut) {
        return ResponseEntity.ok(issueService.getIssuesByStatus(statut));
    }

    /**
     * Supprime une issue.
     * @param id identifiant de l'issue à supprimer
     * @return réponse vide avec statut 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIssue(@PathVariable Long id) {
        issueService.deleteIssue(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Met à jour le statut d'une issue.
     * @param id identifiant de l'issue
     * @param dto nouveau statut
     * @return l'issue mise à jour
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Issue> updateIssueStatus(@PathVariable Long id, @RequestBody UpdateIssueStatusDTO dto) {
        Issue updatedIssue = issueService.updateIssueStatus(id, dto.getStatut());
        return ResponseEntity.ok(updatedIssue);
    }

    /**
     * Abonne l'utilisateur authentifié à une issue.
     * @param id identifiant de l'issue
     * @param raison raison du suivi (INTERESTED, ABLE_TO_HELP, IMPACTED)
     * @param auth utilisateur authentifié
     * @return l'abonnement créé avec statut 201
     */
    @PostMapping("/{id}/follow")
    public ResponseEntity<IssueFollower> followIssue(
            @PathVariable Long id,
            @RequestParam String raison,
            Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        IssueFollower follower = issueFollowerService.followIssue(id, raison, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(follower);
    }

    /**
     * Vérifie si l'utilisateur authentifié suit une issue.
     * @param id identifiant de l'issue
     * @param auth utilisateur authentifié
     * @return true si l'utilisateur suit l'issue
     */
    @GetMapping("/{id}/follow/status")
    public ResponseEntity<Boolean> isFollowing(
            @PathVariable Long id,
            Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ResponseEntity.ok(issueFollowerService.isFollowing(id, user));
    }

    /**
     * Récupère les issues suivies par l'utilisateur authentifié.
     * @param auth utilisateur authentifié
     * @return liste des issues suivies
     */
    @GetMapping("/followed")
    public ResponseEntity<List<Issue>> getFollowedIssues(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ResponseEntity.ok(issueFollowerService.getFollowedIssues(user));
    }

    /**
     * Vérifie si l'utilisateur authentifié peut interagir sur une issue.
     * @param id identifiant de l'issue
     * @param auth utilisateur authentifié
     * @return true si l'utilisateur peut interagir (ABLE_TO_HELP ou IMPACTED)
     */
    @GetMapping("/{id}/can-interact")
    public ResponseEntity<Boolean> canInteract(
            @PathVariable Long id,
            Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ResponseEntity.ok(issueFollowerService.canInteract(id, user));
    }
}