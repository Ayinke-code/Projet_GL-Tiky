package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByCategoryId(Long categoryId);

    List<Issue> findByCreatedById(Long userId);

    List<Issue> findByStatus(IssueStatus status);

    boolean existsByCreatedByIdAndStatus(Long userId, IssueStatus status);

    List<Issue> findByStatusAndCreatedBy_Id(IssueStatus status, Long createdById);

    @Query("SELECT DISTINCT i FROM Issue i " +
            "WHERE i.createdBy = :user " +
            "OR (i.isPrivate = false AND i.status IN :openStatuses) " +
            "OR i.id IN (SELECT f.issue.id FROM IssueFollower f WHERE f.user = :user)")
    List<Issue> findDisplayableIssues(
            @Param("user") User user,
            @Param("openStatuses") List<IssueStatus> openStatuses
    );

}

