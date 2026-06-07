package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.IssueRelation;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.RelationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IssueRelationRepository extends JpaRepository<IssueRelation, Long> {

    List<IssueRelation> findAllBySourceIssueIdOrTargetIssueId(Long sourceId, Long targetId);

    boolean existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(Long sourceId, Long targetId);

    boolean existsBySourceIssueIdAndTargetIssueIdAndCreatedByAndManagerFalse(Long sourceId, Long targetId, User createdBy);

    /**
     * Vérifie si un lien officiel validé existe déjà entre deux issues.
     */
    boolean existsBySourceIssueIdAndTargetIssueIdAndManagerTrueAndStatus(Long sourceId, Long targetId, RelationStatus status);

    /**
     * Récupère tous les liens en attente de validation pour un validateur donné.
     */
    List<IssueRelation> findAllByValidatorAndStatus(User validator, RelationStatus status);
}