package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Conversation;
import fr.horizon.backend_api.model.ConversationUser;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.Message;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.ConversationRole;
import fr.horizon.backend_api.exception.ConversationNotFoundException;
import fr.horizon.backend_api.exception.IssueNotFoundException;
import fr.horizon.backend_api.exception.MessageNotFoundException;
import fr.horizon.backend_api.exception.MessageTooLongException;
import fr.horizon.backend_api.exception.UserNotFoundException;
import fr.horizon.backend_api.repository.ConversationRepository;
import fr.horizon.backend_api.repository.ConversationUserRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.MessageRepository;
import fr.horizon.backend_api.repository.UserRepository;
import fr.horizon.backend_api.util.MapKeys;
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

    // Constants to avoid magic numbers / strings
    private static final int MAX_MESSAGE_LENGTH = 4000;
    private static final String REPLY_TARGET_MUST_SAME_CONV = "Reply target must belong to the same conversation";
    private static final String MISSING_ISSUE_ID = "Missing issue id in payload";

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
        return addMessageToConversation(conversationId, content, user, issueId, null);
    }

    /**
     * Persiste un message dans une conversation liée à une issue, avec réponse optionnelle.
     *
     * @param conversationId identifiant de la conversation
     * @param content contenu du message
     * @param user auteur du message
     * @param issueId identifiant de l'issue liée
     * @param replyToMessageId identifiant du message parent, optionnel
     * @return message enregistré
     */
    public Message addMessageToConversation(Long conversationId, String content, User user, Long issueId, Long replyToMessageId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException(issueId));

        Message.MessageBuilder messageBuilder = Message.builder()
                .content(content)
                .user(user)
                .conversation(conversation)
                .issue(issue);

        if (replyToMessageId != null) {
            Message replyTo = messageRepository.findById(replyToMessageId)
                    .orElseThrow(() -> new MessageNotFoundException(replyToMessageId));

            if (replyTo.getConversation() == null || !conversationId.equals(replyTo.getConversation().getId())) {
                throw new IllegalArgumentException(REPLY_TARGET_MUST_SAME_CONV);
            }

            messageBuilder.replyTo(replyTo);
        }

        Message message = messageBuilder.build();
        Message saved = messageRepository.save(message);

        // Mettre à jour la date de modification de l'issue pour notifier les followers
        issue.setUpdatedAt(java.time.LocalDateTime.now());
        issueRepository.saveAndFlush(issue);

        return saved;
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

        String content = payload.get(MapKeys.CONTENT);
        if (content != null && content.length() > MAX_MESSAGE_LENGTH) {
            throw new MessageTooLongException(MAX_MESSAGE_LENGTH);
        }

        Long issueId = parseMandatoryLong(payload.get(MapKeys.ISSUE_ID));
        Long replyToMessageId = parseOptionalLong(payload.get(MapKeys.REPLY_TO_MESSAGE_ID));
        Message message = addMessageToConversation(conversationId, content, user, issueId, replyToMessageId);

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
        messageMap.put(MapKeys.ID, message.getId());
        messageMap.put(MapKeys.CONTENT, message.getContent());
        messageMap.put(MapKeys.USER, toUserMap(message.getUser()));
        messageMap.put(MapKeys.CREATED_AT, message.getCreatedAt());
        messageMap.put(MapKeys.REPLY_TO_MESSAGE_ID, message.getReplyTo() != null ? message.getReplyTo().getId() : null);
        messageMap.put(MapKeys.REPLY_TO_MESSAGE, message.getReplyTo() != null ? toMessageSummaryMap(message.getReplyTo()) : null);
        return messageMap;
    }

    private Map<String, Object> toMessageSummaryMap(Message message) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put(MapKeys.ID, message.getId());
        messageMap.put(MapKeys.CONTENT, message.getContent());
        messageMap.put(MapKeys.USER, toUserMap(message.getUser()));
        messageMap.put(MapKeys.CREATED_AT, message.getCreatedAt());
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

    public String buildBroadcastPayload(Message message, Long issueId) {
        org.json.JSONObject wsResponse = new org.json.JSONObject();
        wsResponse.put(MapKeys.TYPE, MapKeys.NEW_MESSAGE);
        wsResponse.put(MapKeys.ISSUE_ID, issueId);
        wsResponse.put(MapKeys.MESSAGE, new org.json.JSONObject(toMessageMap(message)));
        return wsResponse.toString();
    }

    private Long parseOptionalLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value);
    }

    private Long parseMandatoryLong(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(MISSING_ISSUE_ID);
        }
        return Long.parseLong(value);
    }
}
