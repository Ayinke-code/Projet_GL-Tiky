package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.*;
import fr.horizon.backend_api.service.IssueService;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.IssueFollower;
import fr.horizon.backend_api.service.IssueFollowerService;
import fr.horizon.backend_api.service.ConversationService;
import fr.horizon.backend_api.websocket.ConversationWebSocketHandler;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur des issues.
 * Expose les endpoints REST permettant de gérer les issues :
 * récupération, création, suppression et suivi.
 *
 * @author Tiky Team
 * @version 1.0
 */
@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
public class IssueController {

    /** Service métier des issues. */
    private final IssueService issueService;

    /** Service métier des abonnements aux issues. */
    private final IssueFollowerService issueFollowerService;

    /** Service des conversations. */
    private final ConversationService conversationService;

    /** Handler WebSocket pour les notifications. */
    private final ConversationWebSocketHandler conversationWebSocketHandler;

    /**
     * Récupère toutes les issues.
     *
     * @return liste des issues sous forme de DTO
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<IssueDto> getAllIssues(Authentication authentication) {
        return issueService.findAll(authentication.getName());
    }

    /**
     * Récupère l'issue correspondant à l'identifiant.
     *
     * @param id identifiant de l'issue
     * @return l'issue sous forme de DTO
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public IssueDto getIssueById(@PathVariable Long id, Authentication authentication) {
        return issueService.findById(id, authentication.getName());
    }

    /**
     * Crée une nouvelle issue.
     * <p>
     * L'utilisateur est récupéré depuis le contexte Spring Security
     * afin d'être défini comme créateur de l'issue.
     *
     * @param dto            données de création de l'issue
     * @param authentication contexte d'authentification Spring Security
     * @return issue créée sous forme de DTO
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public IssueDto createIssue(@RequestBody IssueCreateDto dto, Authentication authentication) {
        String email = authentication.getName();
        return issueService.createIssue(dto, email);
    }

    /**
     * Met à jour le statut d'une issue.
     *
     * @param id     identifiant de l'issue
     * @param status nouveau statut sous forme de chaîne (ex: OUVERT, TERMINE)
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/update-status")
    public void updateStatus(@PathVariable Long id, @RequestBody String status) {
        issueService.updateStatus(status, id);
    }

     /**
      * Inverse la visibilité privée d'une issue.
      * Après la mise à jour, retourne la liste des issues suivies mises à jour
      * pour éviter de devoir refresh la page.
      *
      * @param id identifiant de l'issue
      * @param authentication contexte d'authentification Spring Security
      * @return liste des issues suivies mises à jour
      */
     @PreAuthorize("isAuthenticated()")
     @PostMapping("/{id}/update-private")
     public ResponseEntity<List<IssueDto>> updatePrivacy(@PathVariable Long id, Authentication authentication) {
         issueService.updatePrivacy(id, authentication.getName());

         // Diffusion d'une notification via WebSocket pour forcer le refresh/redirection chez les clients
         conversationService.getConversationByIssueId(id).ifPresent(conv -> {
             String payload = "{\"type\":\"privacy_update\",\"issueId\":" + id + "}";
             conversationWebSocketHandler.broadcastToConversation(conv.getId(), payload);
         });

         User user = issueFollowerService.getUserByEmail(authentication.getName());
         List<IssueDto> updatedFollowedIssues = issueFollowerService.getFollowedIssues(user)
                 .stream()
                 .map(this::convertToDto)
                 .toList();
         return ResponseEntity.ok(updatedFollowedIssues);
     }
     
     /**
      * Convertit une Issue en IssueDto.
      * Utilitaire pour éviter la dépendance circulaire.
      */
     private IssueDto convertToDto(fr.horizon.backend_api.model.Issue issue) {
         User manager = issue.getManager();
         return IssueDto.builder()
                 .id(issue.getId())
                 .title(issue.getTitle())
                 .description(issue.getDescription())
                 .status(issue.getStatus())
                 .isPrivate(issue.isPrivate())
                 .categoryId(issue.getCategory().getId())
                 .categoryName(issue.getCategory().getName())
                 .categoryOwnerId(issue.getCategory().getOwner().getId())
                 .categoryOwnerName(issue.getCategory().getOwner().getName())
                 .categoryManagerId(manager != null ? manager.getId() : null)
                 .categoryManagerName(manager != null ? manager.getName() : null)
                 .createdId(issue.getCreatedBy().getId())
                 .createdName(issue.getCreatedBy().getName())
                 .createdAt(issue.getCreatedAt())
                 .updatedAt(issue.getUpdatedAt())
                 .build();
     }

    /**
     * Supprime une issue par son identifiant.
     *
     * @param id identifiant de l'issue
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public void deleteIssue(@PathVariable Long id, Authentication authentication) {
        issueService.deleteById(id, authentication.getName());
    }

    /**
     * Crée une demande d'assignation de manager pour une issue.
     *
     * @param issueId identifiant de l'issue
     * @param userId  identifiant de l'enseignant cible
     * @return la demande créée sous forme de DTO
     */
    @PreAuthorize("hasAnyAuthority('TEACHER')")
    @PostMapping("assign-manager/issue/{issueId}/target-user/{userId}")
    public ResponseEntity<RequestManagerDto> assignManager(
            @PathVariable Long issueId,
            @PathVariable Long userId) {
        RequestManagerDto dto = issueService.assignManagerToIssue(issueId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Accepte ou refuse une demande d'assignation de manager.
     *
     * @param demandeId identifiant de la demande
     * @param accept    true pour accepter, false pour refuser
     * @param userId    identifiant de l'utilisateur qui répond
     * @return la demande mise à jour sous forme de DTO
     */
    @PreAuthorize("hasAnyAuthority('TEACHER')")
    @PutMapping("demandes-manager/{demandeId}/respond")
    public ResponseEntity<RequestManagerDto> respondToRequest(
            @PathVariable Long demandeId,
            @RequestParam boolean accept,
            @RequestParam Long userId) {
        RequestManagerDto dto = issueService.respondManagerRequest(accept, demandeId, userId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Récupère les demandes d'assignation en attente pour un enseignant.
     *
     * @param userId identifiant de l'enseignant
     * @return liste des demandes en attente
     */
    @PreAuthorize("hasAnyAuthority('TEACHER')")
    @GetMapping("demandes-manager/teacher")
    public ResponseEntity<List<RequestManagerDto>> getTeacherAssignations(@RequestParam Long userId) {
        List<RequestManagerDto> list = issueService.getTeacherAssignations(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * Récupère les alertes pour un propriétaire de catégorie (issues sans manager assigné).
     *
     * @param userId identifiant du propriétaire de catégorie
     * @return liste des alertes
     */
    @PreAuthorize("hasAnyAuthority('TEACHER')")
    @GetMapping("demandes-manager/owner-alerts")
    public ResponseEntity<List<RequestManagerDto>> alertCategoryOwner(@RequestParam Long userId) {
        List<RequestManagerDto> list = issueService.alertCategoryOwner(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * Récupère l'historique des demandes d'assignation.
     *
     * @param userId identifiant de l'utilisateur
     * @return liste des demandes historiques
     */
    @PreAuthorize("hasAnyAuthority('TEACHER')")
    @GetMapping("/demandes-manager/history")
    public ResponseEntity<List<RequestManagerDto>> getHistory(@RequestParam Long userId) {
        List<RequestManagerDto> list = issueService.getHistoricalRequestManager(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * Récupère les issues incomplètes pour un utilisateur.
     *
     * @param userId identifiant de l'utilisateur
     * @return liste des issues incomplètes ou 204 si vide
     */
    @GetMapping("/incomplete/{userId}")
    public ResponseEntity<List<IssueDto>> getIncompleteIssues(@PathVariable Long userId) {
        List<IssueDto> issues = issueService.getIncompleteIssuesToResent(userId);
        if (issues.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(issues);
    }

    /**
     * Marque une issue comme incomplète.
     *
     * @param id                 identifiant de l'issue
     * @param incompleteIssueDto données de la demande d'incomplétude
     * @return message de confirmation ou erreur
     */
    @PutMapping("/{id}/mark-incomplete")
    public ResponseEntity<String> markAsIncomplete(
            @PathVariable Long id,
            @RequestBody IncompleteRequestDto incompleteIssueDto) {
        try {
            issueService.markIssueAsIncomplete(id, incompleteIssueDto.getCommentaire(), incompleteIssueDto.getRaison());
            return ResponseEntity.ok("L'issue a été marquée comme incomplète et la demande a été refusée.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Une erreur inattendue est survenue.");
        }
    }

    /**
     * Récupère une issue sous forme de DTO par son identifiant.
     *
     * @param id identifiant de l'issue
     * @return l'issue sous forme de DTO
     */
    @GetMapping("dto/{id}")
    public ResponseEntity<IssueDto> getIssueDTOById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(issueService.getIssueDTOById(id, authentication.getName()));
    }

    /**
     * Met à jour une issue lorsqu'elle est signalée incomplète.
     * <p>
     * L'utilisateur est récupéré depuis le contexte Spring Security
     * afin d'être identifié comme auteur de la mise à jour.
     *
     * @param dto            données de mise à jour de l'issue
     * @param authentication contexte d'authentification Spring Security
     * @param id             identifiant de l'issue
     * @return issue mise à jour sous forme de DTO
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("update/incomplete/{id}")
    public IssueDto updateIssueWhenIncomplete(
            @RequestBody IssueCreateDto dto,
            Authentication authentication,
            @PathVariable Long id) {
        String email = authentication.getName();
        return issueService.updateIssueWhenIncomplete(email, id, dto);
    }

    /**
     * Abonne l'utilisateur courant à une issue avec une raison.
     *
     * @param id     identifiant de l'issue à suivre
     * @param reason raison du suivi (INTERESTED, ABLE_TO_HELP, IMPACTED)
     * @param auth   contexte d'authentification Spring Security
     * @return l'abonnement créé avec le statut 201
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/follow")
    public ResponseEntity<FollowerDto> followIssue(
            @PathVariable Long id,
            @RequestParam String reason,
            Authentication auth) {
        User user = issueFollowerService.getUserByEmail(auth.getName());
        IssueFollower follower = issueFollowerService.followIssue(id, reason, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(convertToFollowerDto(follower));
    }

    private FollowerDto convertToFollowerDto(IssueFollower follower) {
        return FollowerDto.builder()
                .id(follower.getId())
                .reason(follower.getReason())
                .issueId(follower.getIssue().getId())
                .userId(follower.getUser().getId())
                .userName(follower.getUser().getName())
                .build();
    }

    /**
     * Désabonne l'utilisateur courant d'une issue.
     *
     * @param id   identifiant de l'issue
     * @param auth contexte d'authentification Spring Security
     * @return réponse vide avec le statut 204
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}/follow")
    public ResponseEntity<Void> unfollowIssue(
            @PathVariable Long id,
            Authentication auth) {
        User user = issueFollowerService.getUserByEmail(auth.getName());
        issueFollowerService.unfollowIssue(id, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère la liste des issues suivies par l'utilisateur courant.
     *
     * @param auth contexte d'authentification Spring Security
     * @return liste des issues suivies sous forme de DTO
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/followed")
    public ResponseEntity<List<IssueDto>> getFollowedIssues(Authentication auth) {
        User user = issueFollowerService.getUserByEmail(auth.getName());
        List<IssueDto> dtos = issueFollowerService.getFollowedIssues(user)
                .stream()
                .map(issueService::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Vérifie si l'utilisateur courant suit une issue.
     *
     * @param id   identifiant de l'issue
     * @param auth contexte d'authentification Spring Security
     * @return true si l'utilisateur suit l'issue, false sinon
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/follow/status")
    public ResponseEntity<Boolean> isFollowing(
            @PathVariable Long id,
            Authentication auth) {
        User user = issueFollowerService.getUserByEmail(auth.getName());
        return ResponseEntity.ok(issueFollowerService.isFollowing(id, user));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{issueId}/followers")
    public ResponseEntity<List<FollowersInfoDto>> getIssueFollowers(@PathVariable Long issueId){
        return  ResponseEntity.ok(issueFollowerService.getFollowers(issueId));
    }

    /**
     * Récupère la liste des issues affichage a l'acceuil de la page
     *
     * @param auth contexte d'authentification Spring Security
     * @return liste des issues affichage a l'acceuil
     */
    @GetMapping("/displayable")
    public ResponseEntity<List<IssueDto>> getDisplayableIssues(Authentication auth) {
        String email = auth.getName();
        List<IssueDto> displayableIssues = issueService.getAllDisplayableIssue(email);
        return ResponseEntity.ok(displayableIssues);
    }

}