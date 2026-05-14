package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.RequestManager;
import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import fr.horizon.backend_api.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestManagerRepository extends JpaRepository<RequestManager, Long> {
    boolean existsByIssueAndStatus(Issue issue, RequestManagerStatus status);
}
