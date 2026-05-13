package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Conversation;
import fr.horizon.backend_api.model.ConversationUser;
import fr.horizon.backend_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationUserRepository extends JpaRepository<ConversationUser, Long> {
    List<ConversationUser> findByConversation(Conversation conversation);
    List<ConversationUser> findByUser(User user);
    List<ConversationUser> findByConversationAndUser(Conversation conversation, User user);
}

