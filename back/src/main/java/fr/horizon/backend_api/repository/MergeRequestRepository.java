package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.MergeRequest;
import fr.horizon.backend_api.model.enums.MergeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MergeRequestRepository extends JpaRepository<MergeRequest, Long> {

    // Vérifie si une demande de fusion est déjà en cours entre ces deux tickets
    boolean existsBySourceIssueIdAndTargetIssueIdAndStatus(Long sourceId, Long targetId, MergeRequestStatus status);

    // Récupère toutes les demandes en attente pour un manager spécifique
    @Query("SELECT m FROM MergeRequest m WHERE m.status = 'PENDING' AND " +
            "(m.sourceIssue.manager.id = :managerId OR m.targetIssue.manager.id = :managerId)")
    List<MergeRequest> findPendingRequestsForManager(@Param("managerId") Long managerId);
}