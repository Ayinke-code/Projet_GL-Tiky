package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.IssueFollower;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.FollowedReason;
import fr.horizon.backend_api.repository.IssueFollowerRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service gérant l'abonnement des utilisateurs aux issues.
 * Permet de suivre une issue avec une raison et de se désabonner.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IssueFollowerService {

    private final IssueFollowerRepository issueFollowerRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    /**
     * Abonne un utilisateur à une issue avec une raison.
     *
     * @param issueId identifiant de l'issue à suivre
     * @param reason  raison du suivi (INTERESTED, ABLE_TO_HELP, IMPACTED)
     * @param user    utilisateur qui souhaite suivre l'issue
     * @return l'abonnement créé
     * @throws ResponseStatusException 404 si l'issue n'existe pas
     * @throws ResponseStatusException 409 si l'utilisateur suit déjà l'issue
     * @throws ResponseStatusException 400 si la raison est invalide
     */
    public IssueFollower followIssue(Long issueId, String reason, User user) {

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Issue non trouvée"));

        if (issueFollowerRepository.existsByUserAndIssue(user, issue)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Vous suivez déjà cette issue");
        }

        FollowedReason followedReason;
        try {
            followedReason = FollowedReason.valueOf(reason);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Raison invalide. Valeurs acceptées : INTERESTED, ABLE_TO_HELP, IMPACTED");
        }

        IssueFollower follower = IssueFollower.builder()
                .issue(issue)
                .user(user)
                .reason(followedReason)
                .build();

        return issueFollowerRepository.save(follower);
    }

    /**
     * Désabonne un utilisateur d'une issue.
     *
     * @param issueId identifiant de l'issue
     * @param user    utilisateur qui souhaite se désabonner
     * @throws ResponseStatusException 404 si l'issue n'existe pas
     * @throws ResponseStatusException 404 si l'abonnement n'existe pas
     */
    public void unfollowIssue(Long issueId, User user) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Issue non trouvée"));

        IssueFollower follower = issueFollowerRepository.findByUserAndIssue(user, issue)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Abonnement non trouvé"));

        issueFollowerRepository.delete(follower);
    }

    /**
     * Récupère un utilisateur par son email.
     *
     * @param email email de l'utilisateur
     * @return l'utilisateur trouvé
     * @throws ResponseStatusException 404 si l'utilisateur n'existe pas
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));
    }
}