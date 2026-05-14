package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Document;
import fr.horizon.backend_api.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByIssue(Issue issue);
}