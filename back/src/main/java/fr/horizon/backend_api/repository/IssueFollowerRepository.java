package fr.horizon.backend_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import fr.horizon.backend_api.model.IssueFollower;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.User;
import java.util.List;
import java.util.Optional;

/**
 * Repository gérant les opérations de base de données pour les abonnements aux issues.
 */
public interface IssueFollowerRepository extends JpaRepository<IssueFollower, Long> {

    /**
     * Vérifie si un utilisateur suit déjà une issue.
     * @param createur utilisateur à vérifier
     * @param issue issue concernée
     * @return true si l'utilisateur suit déjà l'issue
     */
    boolean existsByCreateurAndIssue(User createur, Issue issue);

    /**
     * Récupère tous les abonnements d'un utilisateur.
     * @param createur utilisateur dont on veut les abonnements
     * @return liste des abonnements
     */
    List<IssueFollower> findByCreateur(User createur);

    /**
     * Récupère l'abonnement d'un utilisateur à une issue spécifique.
     * @param createur utilisateur concerné
     * @param issue issue concernée
     * @return l'abonnement s'il existe
     */
    Optional<IssueFollower> findByCreateurAndIssue(User createur, Issue issue);
}