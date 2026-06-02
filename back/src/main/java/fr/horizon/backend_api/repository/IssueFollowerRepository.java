package fr.horizon.backend_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import fr.horizon.backend_api.model.IssueFollower;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.IssueStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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
     * Récupère tous les abonnements d'un utilisateur.
     * @param user utilisateur dont on veut les abonnements
     * @return liste des abonnements
     */
    List<IssueFollower> findByUser(User user);

    @Query("""
            select f.issue
            from IssueFollower f
            left join f.issue.createdBy creator
            left join f.issue.manager manager
            left join f.issue.category category
            left join category.owner owner
            where f.user.id = :userId
              and f.issue.status = :openStatus
              and (
                    f.issue.isPrivate = false
                    or creator.id = :userId
                    or manager.id = :userId
                    or owner.id = :userId
              )
            """)
    List<Issue> findAccessibleFollowedIssues(
            @Param("userId") Long userId,
            @Param("openStatus") IssueStatus openStatus);

    @Transactional
    @Modifying
    @Query("""
            delete from IssueFollower f
            where f.user.id = :userId
              and f.issue.isPrivate = true
              and f.issue.createdBy.id <> :userId
              and (f.issue.manager is null or f.issue.manager.id <> :userId)
              and (f.issue.category is null or f.issue.category.owner is null or f.issue.category.owner.id <> :userId)
            """)
    int deleteInaccessibleFollowedIssuesByUser(@Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("""
            delete from IssueFollower f
            where f.issue.id = :issueId
              and f.issue.isPrivate = true
              and f.user.id <> :creatorId
              and (f.issue.manager is null or f.user.id <> f.issue.manager.id)
              and (f.issue.category is null or f.issue.category.owner is null or f.user.id <> f.issue.category.owner.id)
            """)
    int deleteInaccessibleFollowedIssuesByIssue(
            @Param("issueId") Long issueId,
            @Param("creatorId") Long creatorId);

    @Query("SELECT f.user FROM IssueFollower f WHERE f.issue.id = :issueId")
    List<User> findUsersByIssueId(@Param("issueId") Long issueId);

    long countByIssue(Issue issue);
}