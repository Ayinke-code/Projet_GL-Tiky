package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.IssueFollower;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.FollowedReason;
import fr.horizon.backend_api.repository.IssueFollowerRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class IssueFollowerService {

    private final IssueFollowerRepository issueFollowerRepository;
    private final IssueRepository issueRepository;

    public IssueFollower followIssue(Long issueId, String raison, User user) {

        // Récupérer l'issue
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Issue non trouvée"));

        // Vérifier que l'utilisateur ne suit pas déjà cette issue
        System.out.println(">>> User id: " + user.getId() + " Issue id: " + issueId);
        if (issueFollowerRepository.existsByCreateurAndIssue(user, issue)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vous suivez déjà cette issue");
        }

        // Valider et convertir la raison
        FollowedReason followedReason;
        try {
            followedReason = FollowedReason.valueOf(raison);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Raison invalide. Valeurs acceptées : INTERESTED, ABLE_TO_HELP, IMPACTED");
        }

        // Créer l'abonnement
        System.out.println(">>> Création abonnement: " + raison + " pour issue " + issueId);
        IssueFollower follower = IssueFollower.builder()
                .issue(issue)
                .createur(user)
                .raison(followedReason)
                .build();

        return issueFollowerRepository.save(follower);
    }

    public boolean isFollowing(Long issueId, User user) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Issue non trouvée"));
        return issueFollowerRepository.existsByCreateurAndIssue(user, issue);
    }

    public List<Issue> getFollowedIssues(User user) {
        return issueFollowerRepository.findByCreateur(user)
                .stream()
                .map(IssueFollower::getIssue)
                .toList();
    }
}