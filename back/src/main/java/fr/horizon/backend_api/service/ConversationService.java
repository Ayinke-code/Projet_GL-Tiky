package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Conversation;
import fr.horizon.backend_api.model.ConversationUser;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.Message;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.ConversationRole;
import fr.horizon.backend_api.exception.ConversationNotFoundException;
import fr.horizon.backend_api.exception.IssueNotFoundException;
import fr.horizon.backend_api.exception.MessageTooLongException;
import fr.horizon.backend_api.exception.UserNotFoundException;
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
/**
 * Service métier des conversations.
 * <p>
 * Centralise la récupération des conversations, la gestion des participants
 * et la création des messages associés à une issue.
 * <p>
 * Le service prépare également les structures de données destinées
 * à la réponse REST et au broadcast WebSocket, afin de garder le contrôleur fin.
 */
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationUserRepository conversationUserRepository;
    private final MessageRepository messageRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    /**
     * Résultat de l'envoi d'un message : payload REST, payload WebSocket et issue ciblée.
     */
    public record MessageDispatchResult(Map<String, Object> message, String broadcastPayload, Long issueId) {
    }

    /**
     * Récupère la conversation liée à une issue.
     *
     * @param issueId identifiant de l'issue
     * @return conversation si elle existe, vide sinon
     */
    public Optional<Conversation> getConversationByIssueId(Long issueId) {
        return conversationRepository.findByIssueId(issueId);
    }

    /**
     * Crée une conversation pour une issue donnée.
     *
     * @param issue issue concernée
     * @return conversation créée
     */
    public Conversation createConversationForIssue(Issue issue) {
        Conversation conversation = Conversation.builder()
                .issue(issue)
                .build();
        return conversationRepository.save(conversation);
    }

    /**
     * Récupère ou crée une conversation pour une issue (idempotent).
     * <p>
     * Si la conversation n'existe pas, elle est créée.
     * Si elle existe déjà, elle est retournée.
     *
     * @param issueId identifiant de l'issue
     * @return map contenant la clé {@code conversation} avec la conversation sérialisée
     */
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> getOrCreateConversationForIssue(Long issueId) {
        Optional<Conversation> existing = getConversationByIssueId(issueId);

        if (existing.isPresent()) {
            // Conversation existe déjà
            Map<String, Object> response = new HashMap<>();
            response.put(fr.horizon.backend_api.util.MapKeys.CONVERSATION, toConversationMap(existing.get()));
            return response;
        }

        // Créer une nouvelle conversation
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException(issueId));

        Conversation conversation = createConversationForIssue(issue);

        Map<String, Object> response = new HashMap<>();
        response.put(fr.horizon.backend_api.util.MapKeys.CONVERSATION, toConversationMap(conversation));
        return response;
    }

    /**
     * Ajoute un utilisateur à une conversation avec un rôle métier donné.
     *
     * @param conversation conversation cible
     * @param user utilisateur à associer
     * @param role rôle à attribuer dans la conversation
     */
    public void addUserToConversation(Conversation conversation, User user, String role) {
        ConversationUser conversationUser = ConversationUser.builder()
                .conversation(conversation)
                .user(user)
                .role(ConversationRole.valueOf(role))
                .build();
        conversationUserRepository.save(conversationUser);
    }

    /**
     * Persiste un message dans une conversation liée à une issue.
     *
     * @param conversationId identifiant de la conversation
     * @param content contenu du message
     * @param user auteur du message
     * @param issueId identifiant de l'issue liée
     * @return message enregistré
     */
    public Message addMessageToConversation(Long conversationId, String content, User user, Long issueId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException(issueId));

        Message message = Message.builder()
                .content(content)
                .user(user)
                .conversation(conversation)
                .issue(issue)
                .build();

        return messageRepository.save(message);
    }

    /**
     * Enregistre un message à partir des données de requête HTTP.
     * <p>
     * La méthode prépare ensuite le payload REST et le payload de broadcast WebSocket.
     *
     * @param conversationId identifiant de la conversation
     * @param payload données reçues depuis le front
     * @param email email de l'utilisateur authentifié
     * @return résultat contenant le message sérialisé et le payload de diffusion
     */
    public MessageDispatchResult addMessage(Long conversationId, Map<String, String> payload, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        String content = payload.get("content");
        if (content != null && content.length() > 4000) {
            throw new MessageTooLongException(4000);
        }

        Long issueId = Long.parseLong(payload.get("issueId"));
        Message message = addMessageToConversation(conversationId, content, user, issueId);

        return new MessageDispatchResult(toMessageMap(message), buildBroadcastPayload(message, issueId), issueId);
    }

    /**
     * Construit la réponse complète d'une conversation associée à une issue.
     *
     * @param issueId identifiant de l'issue
     * @return map contenant la clé {@code conversation}
     */
    public Map<String, Object> getConversationByIssuePayload(Long issueId) {
        Map<String, Object> response = new HashMap<>();
        Optional<Conversation> conversation = getConversationByIssueId(issueId);

        if (conversation.isEmpty()) {
            response.put(fr.horizon.backend_api.util.MapKeys.CONVERSATION, null);
            return response;
        }

        response.put(fr.horizon.backend_api.util.MapKeys.CONVERSATION, toConversationMap(conversation.get()));
        return response;
    }

    /**
     * Récupère les messages d'une conversation, triés par date croissante.
     *
     * @param conversationId identifiant de la conversation
     * @return liste des messages
     */
    public List<Message> getMessagesForConversation(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }

    /**
     * Récupère les utilisateurs associés à une conversation.
     *
     * @param conversationId identifiant de la conversation
     * @return liste des participants
     */
    public List<ConversationUser> getUsersInConversation(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
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
            userMap.put(fr.horizon.backend_api.util.MapKeys.CONVERSATION_ROLE, conversationUser.getRole().toString());
            usersList.add(userMap);
        }

        Map<String, Object> conversationMap = new HashMap<>();
        conversationMap.put(fr.horizon.backend_api.util.MapKeys.ID, conversation.getId());
        conversationMap.put(fr.horizon.backend_api.util.MapKeys.MESSAGES, messagesList);
        conversationMap.put(fr.horizon.backend_api.util.MapKeys.USERS, usersList);
        conversationMap.put(fr.horizon.backend_api.util.MapKeys.CREATED_AT, conversation.getCreatedAt());
        return conversationMap;
    }

    private Map<String, Object> toMessageMap(Message message) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put(fr.horizon.backend_api.util.MapKeys.ID, message.getId());
        messageMap.put(fr.horizon.backend_api.util.MapKeys.CONTENT, message.getContent());
        messageMap.put(fr.horizon.backend_api.util.MapKeys.USER, toUserMap(message.getUser()));
        messageMap.put(fr.horizon.backend_api.util.MapKeys.CREATED_AT, message.getCreatedAt());
        return messageMap;
    }

    private Map<String, Object> toUserMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put(fr.horizon.backend_api.util.MapKeys.ID, user.getId());
        userMap.put(fr.horizon.backend_api.util.MapKeys.NAME, user.getName());
        userMap.put(fr.horizon.backend_api.util.MapKeys.EMAIL, user.getEmail());
        userMap.put(fr.horizon.backend_api.util.MapKeys.DISPLAY_ROLE, user.getDisplayRole());
        userMap.put(fr.horizon.backend_api.util.MapKeys.ROLES, user.getRoleNames());
        return userMap;
    }

    private String buildBroadcastPayload(Message message, Long issueId) {
        org.json.JSONObject wsResponse = new org.json.JSONObject();
        wsResponse.put(fr.horizon.backend_api.util.MapKeys.TYPE, fr.horizon.backend_api.util.MapKeys.NEW_MESSAGE);
        wsResponse.put(fr.horizon.backend_api.util.MapKeys.ISSUE_ID, issueId);

        org.json.JSONObject msgJson = new org.json.JSONObject();
        msgJson.put(fr.horizon.backend_api.util.MapKeys.ID, message.getId());
        msgJson.put(fr.horizon.backend_api.util.MapKeys.CONTENT, message.getContent());
        msgJson.put(fr.horizon.backend_api.util.MapKeys.CREATED_AT, message.getCreatedAt());

        org.json.JSONObject userJson = new org.json.JSONObject();
        userJson.put(fr.horizon.backend_api.util.MapKeys.ID, message.getUser().getId());
        userJson.put(fr.horizon.backend_api.util.MapKeys.NAME, message.getUser().getName());
        userJson.put(fr.horizon.backend_api.util.MapKeys.EMAIL, message.getUser().getEmail());
        userJson.put(fr.horizon.backend_api.util.MapKeys.DISPLAY_ROLE, message.getUser().getDisplayRole());
        org.json.JSONArray roles = new org.json.JSONArray();
        message.getUser().getRoleNames().forEach(roles::put);
        userJson.put(fr.horizon.backend_api.util.MapKeys.ROLES, roles);
        msgJson.put(fr.horizon.backend_api.util.MapKeys.USER, userJson);

        wsResponse.put(fr.horizon.backend_api.util.MapKeys.MESSAGE, msgJson);
        return wsResponse.toString();
    }
}

