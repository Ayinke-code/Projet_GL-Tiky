package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.DemandeManager;
import fr.horizon.backend_api.model.DemandeManagerStatus;
import fr.horizon.backend_api.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemandeManagerRepository extends JpaRepository<DemandeManager, Long> {
    boolean existsByIssueAndStatut(Issue issue, DemandeManagerStatus statut);
}
