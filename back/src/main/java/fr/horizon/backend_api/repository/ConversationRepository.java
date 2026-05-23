package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Conversation;
import fr.horizon.backend_api.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByIssue(Issue issue);
    Optional<Conversation> findByIssueId(Long issueId);
}

