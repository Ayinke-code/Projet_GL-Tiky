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

/**
 * Service gérant l'abonnement des utilisateurs aux issues.
 * Permet de suivre une issue avec une raison et d'appliquer les droits selon la raison choisie.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IssueFollowerService {

    private final IssueFollowerRepository issueFollowerRepository;
    private final IssueRepository issueRepository;

    /**
     * Abonne un utilisateur à une issue avec une raison.
     *
     * @param issueId identifiant de l'issue à suivre
     * @param raison  raison du suivi (INTERESTED, ABLE_TO_HELP, IMPACTED)
     * @param user    utilisateur qui souhaite suivre l'issue
     * @return l'abonnement créé
     * @throws ResponseStatusException 404 si l'issue n'existe pas
     * @throws ResponseStatusException 409 si l'utilisateur suit déjà l'issue
     * @throws ResponseStatusException 400 si la raison est invalide
     */
    public IssueFollower followIssue(Long issueId, String raison, User user) {

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

        // Valider et convertir la raison
        FollowedReason followedReason;
        try {
            followedReason = FollowedReason.valueOf(raison);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Raison invalide. Valeurs acceptées : INTERESTED, ABLE_TO_HELP, IMPACTED");
        }

        // Créer et sauvegarder l'abonnement
        IssueFollower follower = IssueFollower.builder()
                .issue(issue)
                .createur(user)
                .raison(followedReason)
                .build();

        return issueFollowerRepository.save(follower);
    }
}