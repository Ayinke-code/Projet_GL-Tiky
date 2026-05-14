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
    boolean existsByIssueAndStatusAndTargetTeacherIsNotNull(Issue issue, RequestManagerStatus status);
    List<RequestManager> findByTargetTeacher_Id(Long targetTeacherId);
    Optional<RequestManager> findFirstByIssueOrderByCreatedAtDesc(Issue issue);
    List<RequestManager> findByCategoryOwner_IdAndTargetTeacherIsNull(long id);
    List<RequestManager> findByCategoryOwner_IdAndTargetTeacherIsNotNullOrderByCreatedAtDesc(Long userId);
}
