package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Document;
import fr.horizon.backend_api.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByIssueId(Long issueId);
    void deleteByIssueId(Long issueId);
    List<Document> findByIssueIdIn(List<Long> issueIds);
    void deleteByIssueIdIn(List<Long> issueIds);

    @Modifying
    @Query("UPDATE Document d SET d.issue = :target WHERE d.issue = :source")
    void moveDocumentsToNewIssue(@Param("source") Issue source, @Param("target") Issue target);
}
