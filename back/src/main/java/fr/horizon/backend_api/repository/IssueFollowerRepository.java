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
     *
     * @param user utilisateur à vérifier
     * @param issue    issue concernée
     * @return true si l'utilisateur suit déjà l'issue
     */
    boolean existsByUserAndIssue(User user, Issue issue);

    /**
    * Récupère l'abonnement d'un utilisateur à une issue spécifique
    *
    * @param user  utilisateur concerné
    * @param issue issue concernée
    * @return l'abonnement s'il existe
    */
    Optional<IssueFollower> findByUserAndIssue(User user, Issue issue);

    /**
     * Récupère les abonnements d'une issue en excluant un utilisateur spécifique.
     *
     * @param user  utilisateur à exclure
     * @param issue issue concernée
     * @return liste des abonnements
     */
    List<IssueFollower> findByUserNotAndIssue(User user, Issue issue);

    /**
     * Récupère tous les abonnements d'un utilisateur.
     * @param user utilisateur dont on veut les abonnements
     * @return liste des abonnements
     */
    List<IssueFollower> findByUser(User user);

    /**
     * Récupère tous les abonnements d'une issue.
     * @param issue issue concernée
     * @return liste des abonnements
     */
    List<IssueFollower> findByIssue(Issue issue);
}