package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.*;
import fr.horizon.backend_api.service.IssueService;
import lombok.RequiredArgsConstructor;
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
    public List<IssueDto> getAllIssues() {
        return issueService.findAll();
    }

    /**
     * Récupère l'issue correspondant à l'id.
     *
     * @return l'issue sous forme de DTO
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public IssueDto getIssueById(@PathVariable Long id) {
        return issueService.findById(id);
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
    public void updatePrivacy(@PathVariable Long id) {
        issueService.updatePrivacy(id);
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

}
