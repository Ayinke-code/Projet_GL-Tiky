package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.OfficialLinkRequest;
import fr.horizon.backend_api.model.enums.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfficialLinkRequestRepository extends JpaRepository<OfficialLinkRequest, Long> {

    boolean existsBySourceIssueIdAndTargetIssueIdAndTypeAndIsHandledFalse(Long sourceId, Long targetId, RelationType type);

    /**
     * Récupère toutes les demandes non traitées où l'utilisateur est manager
     * du ticket source OU du ticket cible.
     */
    @Query("SELECT r FROM OfficialLinkRequest r WHERE r.isHandled = false AND " +
            "(r.sourceIssue.manager.id = :managerId OR r.targetIssue.manager.id = :managerId)")
    List<OfficialLinkRequest> findPendingRequestsForManager(@Param("managerId") Long managerId);
}