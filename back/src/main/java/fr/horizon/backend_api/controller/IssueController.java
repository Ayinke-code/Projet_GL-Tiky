package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.User;
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
        String email = auth.getName();
        User creator = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

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

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Issue>> getIssuesByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(issueService.getIssuesByCategory(categoryId));
    }

    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<List<Issue>> getIssuesByCreator(@PathVariable Long creatorId) {
        return ResponseEntity.ok(issueService.getIssuesByCreator(creatorId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Issue>> getIssuesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(issueService.getIssuesByStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIssue(@PathVariable Long id) {
        issueService.deleteIssue(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Issue> updateIssueStatus(@PathVariable Long id, @RequestBody UpdateIssueStatusDTO dto) {
        Issue updatedIssue = issueService.updateIssueStatus(id, dto.getStatus());
        return ResponseEntity.ok(updatedIssue);
    }
}
