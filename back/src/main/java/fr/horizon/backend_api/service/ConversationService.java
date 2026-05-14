package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationUserRepository conversationUserRepository;
    private final MessageRepository messageRepository;
    private final IssueRepository issueRepository;

    public Optional<Conversation> getConversationByIssueId(Long issueId) {
        return conversationRepository.findByIssueId(issueId);
    }

    public Conversation createConversationForIssue(Issue issue) {
        Conversation conversation = Conversation.builder()
                .issue(issue)
                .build();
        return conversationRepository.save(conversation);
    }

    public void addUserToConversation(Conversation conversation, User user, String role) {
        ConversationUser conversationUser = ConversationUser.builder()
                .conversation(conversation)
                .user(user)
                .role(fr.horizon.backend_api.model.enums.ConversationRole.valueOf(role))
                .build();
        conversationUserRepository.save(conversationUser);
    }

    public Message addMessageToConversation(Long conversationId, String content, User user, Long issueId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        Message message = Message.builder()
                .content(content)
                .user(user)
                .conversation(conversation)
                .issue(issue)
                .build();

        return messageRepository.save(message);
    }

    public List<Message> getMessagesForConversation(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }

    public List<ConversationUser> getUsersInConversation(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        return conversationUserRepository.findByConversation(conversation);
    }
}