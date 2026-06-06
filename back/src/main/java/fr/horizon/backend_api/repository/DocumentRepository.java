package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByIssueId(Long issueId);
    void deleteByIssueId(Long issueId);
}
