package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Conversation;
import fr.horizon.backend_api.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversation(Conversation conversation);
    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);
}