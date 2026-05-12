package fr.horizon.backend_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import fr.horizon.backend_api.model.IssueFollower;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.User;
import java.util.List;
import java.util.Optional;

public interface IssueFollowerRepository extends JpaRepository<IssueFollower, Long> {

    // Vérifier si un utilisateur suit déjà une issue
    boolean existsByCreateurAndIssue(User createur, Issue issue);

    // Récupérer tous les abonnements d'un utilisateur
    List<IssueFollower> findByCreateur(User createur);

    Optional<IssueFollower> findByCreateurAndIssue(User createur, Issue issue);
}