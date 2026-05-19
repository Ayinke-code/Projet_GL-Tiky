package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Conversation;
import fr.horizon.backend_api.model.ConversationUser;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.Message;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.ConversationRole;
import fr.horizon.backend_api.repository.ConversationRepository;
import fr.horizon.backend_api.repository.ConversationUserRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.MessageRepository;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationUserRepository conversationUserRepository;
    private final MessageRepository messageRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    public record MessageDispatchResult(Map<String, Object> message, String broadcastPayload, Long issueId) {
    }

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
                .role(ConversationRole.valueOf(role))
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

    /**
     * Enregistre un message à partir de la requête HTTP, puis renvoie les données nécessaires
     * à la réponse REST et au broadcast websocket.
     */
    public MessageDispatchResult addMessage(Long conversationId, Map<String, String> payload, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String content = payload.get("content");
        if (content != null && content.length() > 4000) {
            throw new RuntimeException("Le message dépasse la limite de 4000 caractères");
        }

        Long issueId = Long.parseLong(payload.get("issueId"));
        Message message = addMessageToConversation(conversationId, content, user, issueId);

        return new MessageDispatchResult(toMessageMap(message), buildBroadcastPayload(message, issueId), issueId);
    }

    /**
     * Construit la réponse complète d'une conversation associée à une issue.
     */
    public Map<String, Object> getConversationByIssuePayload(Long issueId) {
        Map<String, Object> response = new HashMap<>();
        Optional<Conversation> conversation = getConversationByIssueId(issueId);

        if (conversation.isEmpty()) {
            response.put("conversation", null);
            return response;
        }

        response.put("conversation", toConversationMap(conversation.get()));
        return response;
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

    private Map<String, Object> toConversationMap(Conversation conversation) {
        List<Message> messages = getMessagesForConversation(conversation.getId());
        List<Map<String, Object>> messagesList = new java.util.ArrayList<>();
        for (Message message : messages) {
            messagesList.add(toMessageMap(message));
        }

        List<ConversationUser> conversationUsers = getUsersInConversation(conversation.getId());
        List<Map<String, Object>> usersList = new java.util.ArrayList<>();
        for (ConversationUser conversationUser : conversationUsers) {
            Map<String, Object> userMap = toUserMap(conversationUser.getUser());
            userMap.put("conversationRole", conversationUser.getRole().toString());
            usersList.add(userMap);
        }

        Map<String, Object> conversationMap = new HashMap<>();
        conversationMap.put("id", conversation.getId());
        conversationMap.put("messages", messagesList);
        conversationMap.put("users", usersList);
        conversationMap.put("createdAt", conversation.getCreatedAt());
        return conversationMap;
    }

    private Map<String, Object> toMessageMap(Message message) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("id", message.getId());
        messageMap.put("content", message.getContent());
        messageMap.put("user", toUserMap(message.getUser()));
        messageMap.put("createdAt", message.getCreatedAt());
        return messageMap;
    }

    private Map<String, Object> toUserMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getDisplayRole());
        userMap.put("roles", user.getRoleNames());
        return userMap;
    }

    private String buildBroadcastPayload(Message message, Long issueId) {
        org.json.JSONObject wsResponse = new org.json.JSONObject();
        wsResponse.put("type", "new_message");
        wsResponse.put("issueId", issueId);

        org.json.JSONObject msgJson = new org.json.JSONObject();
        msgJson.put("id", message.getId());
        msgJson.put("content", message.getContent());
        msgJson.put("createdAt", message.getCreatedAt());

        org.json.JSONObject userJson = new org.json.JSONObject();
        userJson.put("id", message.getUser().getId());
        userJson.put("name", message.getUser().getName());
        userJson.put("email", message.getUser().getEmail());
        org.json.JSONArray roles = new org.json.JSONArray();
        message.getUser().getRoleNames().forEach(roles::put);
        userJson.put("roles", roles);
        msgJson.put("user", userJson);

        wsResponse.put("message", msgJson);
        return wsResponse.toString();
    }
}

