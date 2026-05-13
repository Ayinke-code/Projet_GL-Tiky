package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByCategoryId(Long categoryId);

    List<Issue> findByCreatedById(Long userId);

    List<Issue> findByStatus(IssueStatus status);
}

