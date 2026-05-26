package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.*;
import fr.horizon.backend_api.service.IssueService;
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
 * récupération, création et suppression.
 *
 * @author Tiky Team
 * @version 1.0
 */
@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
public class IssueController {

    /**
     * Service métier des issues.
     */
    private final IssueService issueService;

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
     * Récupère l'issue correspondant à l'id.
     *
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
     * @param id identifiant de l'issue
     * @param status nouveau statut sous forme de chaîne (ex: OUVERT, TERMINE)
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/update-status")
    public void updateStatus(@PathVariable Long id, @RequestBody String status) {
        issueService.updateStatus(status, id);
    }

    /**
     * Inverse la visibilité privée d'une issue.
     *
     * @param id identifiant de l'issue
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/update-private")
    public void updatePrivacy(@PathVariable Long id, Authentication authentication) {
        issueService.updatePrivacy(id, authentication.getName());
    }

    /**
     * Supprime une issue par son identifiant.
     *
     * @param id identifiant de l'issue
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TEACHER')")
    @DeleteMapping("/{id}")
    public void deleteIssue(@PathVariable Long id) {
        issueService.deleteById(id);
    }

    /**
     * Endpoint pour créer une demande d'assignation de manager.
     * URL : POST /assign-manager/issue/12/target-user/5
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
     * Endpoint pour accepter ou refuser une demande.
     * URL : PUT /demandes-manager/42/respond?accept=true&userId=5
     */
    @PreAuthorize("hasAnyAuthority('TEACHER')")
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
    @PreAuthorize("hasAnyAuthority('TEACHER')")
    @GetMapping("demandes-manager/teacher")
    public ResponseEntity<List<RequestManagerDto>> getTeacherAssignations(@RequestParam Long userId) {
        List<RequestManagerDto> list = issueService.getTeacherAssignations(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * Récupère les alertes pour un propriétaire de catégorie (tickets sans manager assigné).
     * URL : GET issues/demandes-manager/owner-alerts?userId=1
     */
    @PreAuthorize("hasAnyAuthority('TEACHER')")
    @GetMapping("demandes-manager/owner-alerts")
    public ResponseEntity<List<RequestManagerDto>> alertCategoryOwner(@RequestParam Long userId) {
        List<RequestManagerDto> list = issueService.alertCategoryOwner(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * Récupère l'historique des demandes (Acceptées, Refusées, ou En attente avec cible)
     * URL : GET /issues/demandes-manager/history?userId=1
     */
    @PreAuthorize("hasAnyAuthority('TEACHER')")
    @GetMapping("/demandes-manager/history")
    public ResponseEntity<List<RequestManagerDto>> getHistory(@RequestParam Long userId) {
        List<RequestManagerDto> list = issueService.getHistoricalRequestManager(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * Récupère la liste des issues incomplètes pour un utilisateur spécifique
     */
    @GetMapping("/incomplete/{userId}")
    public ResponseEntity<List<IssueDto>> getIncompleteIssues(@PathVariable Long userId) {
        List<IssueDto> issues = issueService.getIncompleteIssuesToResent(userId);

        if (issues.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(issues);
    }

    @PutMapping("/{id}/mark-incomplete")
    public ResponseEntity<String> markAsIncomplete(@PathVariable Long id, @RequestBody IncompleteRequestDto incompleteIssueDto) {
        try {
            issueService.markIssueAsIncomplete(id, incompleteIssueDto.getCommentaire(), incompleteIssueDto.getRaison() );
            return ResponseEntity.ok("L'issue a été marquée comme incomplète et la demande a été refusée.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Une erreur inattendue est survenue.");
        }
    }

    @GetMapping("dto/{id}")
    public ResponseEntity<IssueDto> getIssueDTOById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(issueService.getIssueDTOById(id, authentication.getName()));
    }

    /**
     * Met a jour une issue lorsqu'elle est signalée imcomplète
     * <p>
     * L'utilisateur est récupéré depuis le contexte Spring Security
     * afin d'être défini comme créateur de l'issue.
     *
     * @param dto            données de création de l'issue
     * @param authentication contexte d'authentification Spring Security
     * @return issue mis à jour sous forme de DTO
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("update/incomplete/{id}")
    public IssueDto updateIssueWhenIncomplete(@RequestBody IssueCreateDto dto, Authentication authentication, @PathVariable Long id) {
        String email = authentication.getName();
        return issueService.updateIssueWhenIncomplete(email, id, dto);
    }



}
