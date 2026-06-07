package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.IssueRelation;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IssueRelationRepository extends JpaRepository<IssueRelation, Long> {

    List<IssueRelation> findAllBySourceIssueIdOrTargetIssueId(Long sourceId, Long targetId);

    boolean existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(Long sourceId, Long targetId);

    boolean existsBySourceIssueIdAndTargetIssueIdAndCreatedByAndManagerFalse(Long sourceId, Long targetId, User createdBy);

    @Query("SELECT COUNT(ir) FROM IssueRelation ir WHERE " +
            "((ir.sourceIssue.id = :issue1 AND ir.targetIssue.id = :issue2) OR " +
            "(ir.sourceIssue.id = :issue2 AND ir.targetIssue.id = :issue1)) " +
            "AND ir.manager = false AND ir.type = :type")
    int countNonManagerRelationsByType(
            @Param("issue1") Long issue1,
            @Param("issue2") Long issue2,
            @Param("type") RelationType type
    );
}