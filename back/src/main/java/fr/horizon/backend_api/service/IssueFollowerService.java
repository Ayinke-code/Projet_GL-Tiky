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

    // Suivre une issue
    public IssueFollower followIssue(Long issueId, int raison, User user) {

        // Récupérer l'issue
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Issue non trouvée"));

        // Vérifier que l'utilisateur ne suit pas déjà cette issue
        if (issueFollowerRepository.existsByCreateurAndIssue(user, issue)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vous suivez déjà cette issue");
        }

        // Créer l'abonnement
        IssueFollower follower = IssueFollower.builder()
                .issue(issue)
                .createur(user)
                .raison(FollowedReason.values()[raison])
                .build();

        return issueFollowerRepository.save(follower);
    }

    // Récupérer les issues suivies par un utilisateur
    public List<Issue> getFollowedIssues(User user) {
        return issueFollowerRepository.findByCreateur(user)
                .stream()
                .map(IssueFollower::getIssue)
                .toList();
    }

}