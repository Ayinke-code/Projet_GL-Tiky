package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.RequestManager;
import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import fr.horizon.backend_api.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestManagerRepository extends JpaRepository<RequestManager, Long> {
    List<RequestManager> findByCategoryOwner_IdAndTargetTeacherIsNotNullOrderByCreatedAtDesc(Long userId);
    List<RequestManager> findByCategoryOwner_IdAndTargetTeacherIsNullAndStatusOrderByCreatedAtDesc(
            Long userId,
            RequestManagerStatus status
    );
    Optional<RequestManager> findByIssue_Id(Long issueId);
}
