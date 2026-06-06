package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.IssueRelation;
import fr.horizon.backend_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IssueRelationRepository extends JpaRepository<IssueRelation, Long> {

    List<IssueRelation> findAllBySourceIssueIdOrTargetIssueId(Long sourceId, Long targetId);

    boolean existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(Long sourceId, Long targetId);

    boolean existsBySourceIssueIdAndTargetIssueIdAndCreatedByAndManagerFalse(Long sourceId, Long targetId, User createdBy);
}