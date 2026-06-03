package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.IssueRelation;
import fr.horizon.backend_api.model.enums.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IssueRelationRepository extends JpaRepository<IssueRelation, Long> {

    @Query("SELECT r FROM IssueRelation r WHERE r.sourceIssue.id = :issueId OR r.targetIssue.id = :issueId")
    List<IssueRelation> findAllByIssueId(@Param("issueId") Long issueId);

    boolean existsBySourceIssueIdAndTargetIssueIdAndType(Long sourceId, Long targetId, RelationType type);

    List<IssueRelation> findAllBySourceIssueIdOrTargetIssueId(Long sourceId, Long targetId);
}