package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}
