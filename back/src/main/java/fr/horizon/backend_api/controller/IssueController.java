package fr.horizon.backend_api.controller;


import fr.horizon.backend_api.model.dto.IssueDto;
import fr.horizon.backend_api.model.dto.RequestManagerDto;
import fr.horizon.backend_api.service.IssueService;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
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
    private final UserRepository userRepository;

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
    public ResponseEntity<String> markAsIncomplete(@PathVariable Long id) {
        try {
            issueService.markIssueAsIncomplete(id);
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
     * Récupère l'historique des demandes (Acceptées, Refusées, ou En attente avec cible)
     * URL : GET /issues/demandes-manager/history?userId=1
     */
    @GetMapping("/demandes-manager/history")
    public ResponseEntity<List<RequestManagerDto>> getHistory(@RequestParam Long userId) {
        List<RequestManagerDto> list = issueService.getHistoricalRequestManager(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("dto/{id}")
    public ResponseEntity<IssueDto> getIssueDTOById(@PathVariable Long id) {
        return ResponseEntity.ok(issueService.getIssueDTOById(id));
    }



}
