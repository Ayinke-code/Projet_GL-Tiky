package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.ConversationNotFoundException;
import fr.horizon.backend_api.exception.IssueNotFoundException;
import fr.horizon.backend_api.exception.MessageNotFoundException;
import fr.horizon.backend_api.exception.MessageTooLongException;
import fr.horizon.backend_api.exception.UserNotFoundException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationUserRepository conversationUserRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void getConversationByIssueId_should_delegate_to_repository() {
        Conversation conversation = Conversation.builder().id(10L).build();
        when(conversationRepository.findByIssueId(5L)).thenReturn(Optional.of(conversation));

        Optional<Conversation> result = conversationService.getConversationByIssueId(5L);

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getId());
        verify(conversationRepository).findByIssueId(5L);
    }

    @Test
    void createConversationForIssue_should_save_conversation_with_issue() {
        Issue issue = new Issue();
        issue.setId(7L);

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Conversation result = conversationService.createConversationForIssue(issue);

        assertNotNull(result);
        assertSame(issue, result.getIssue());
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void addUserToConversation_should_save_conversation_user_with_role() {
        Conversation conversation = Conversation.builder().id(1L).build();
        User user = new User();
        user.setId(2L);

        conversationService.addUserToConversation(conversation, user, "INITIATOR");

        verify(conversationUserRepository).save(org.mockito.ArgumentMatchers.argThat((ConversationUser cu) ->
                cu.getConversation() == conversation
                        && cu.getUser() == user
                        && cu.getRole() == ConversationRole.INITIATOR
        ));
    }

    @Test
    void addMessageToConversation_should_save_message_when_entities_exist() {
        Conversation conversation = Conversation.builder().id(11L).build();
        Issue issue = new Issue();
        issue.setId(22L);
        User user = new User();
        user.setId(33L);

        when(conversationRepository.findById(11L)).thenReturn(Optional.of(conversation));
        when(issueRepository.findById(22L)).thenReturn(Optional.of(issue));
        when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Message result = conversationService.addMessageToConversation(11L, "Hello", user, 22L);

        assertNotNull(result);
        assertEquals("Hello", result.getContent());
        assertSame(user, result.getUser());
        assertSame(conversation, result.getConversation());
        assertSame(issue, result.getIssue());
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void addMessageToConversation_should_throw_when_conversation_missing() {
        when(conversationRepository.findById(11L)).thenReturn(Optional.empty());

        User user = new User();
        org.junit.jupiter.api.function.Executable executable = () -> conversationService.addMessageToConversation(11L, "Hello", user, 22L);
        ConversationNotFoundException ex = assertThrows(ConversationNotFoundException.class, executable);

        assertEquals("Conversation not found with id: 11", ex.getMessage());
    }

    @Test
    void addMessageToConversation_should_throw_when_issue_missing() {
        Conversation conversation = Conversation.builder().id(11L).build();
        when(conversationRepository.findById(11L)).thenReturn(Optional.of(conversation));
        when(issueRepository.findById(22L)).thenReturn(Optional.empty());

        User user = new User();
        org.junit.jupiter.api.function.Executable executable = () -> conversationService.addMessageToConversation(11L, "Hello", user, 22L);
        IssueNotFoundException ex = assertThrows(IssueNotFoundException.class, executable);

        assertEquals("Issue not found with id: 22", ex.getMessage());
    }

    @Test
    void getMessagesForConversation_should_return_ordered_messages() {
        Conversation conversation = Conversation.builder().id(11L).build();
        Message message = Message.builder().id(1L).content("Hello").build();

        when(conversationRepository.findById(11L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationOrderByCreatedAtAsc(conversation)).thenReturn(List.of(message));

        List<Message> result = conversationService.getMessagesForConversation(11L);

        assertEquals(1, result.size());
        assertEquals("Hello", result.get(0).getContent());
        verify(messageRepository).findByConversationOrderByCreatedAtAsc(conversation);
    }

    @Test
    void getUsersInConversation_should_return_users() {
        Conversation conversation = Conversation.builder().id(11L).build();
        ConversationUser conversationUser = ConversationUser.builder()
                .conversation(conversation)
                .build();

        when(conversationRepository.findById(11L)).thenReturn(Optional.of(conversation));
        when(conversationUserRepository.findByConversation(conversation)).thenReturn(List.of(conversationUser));

        List<ConversationUser> result = conversationService.getUsersInConversation(11L);

        assertEquals(1, result.size());
        assertSame(conversation, result.get(0).getConversation());
        verify(conversationUserRepository).findByConversation(conversation);
    }

    @Test
    void getConversationByIssuePayload_should_return_null_when_conversation_missing() {
        when(conversationRepository.findByIssueId(12L)).thenReturn(Optional.empty());

        Map<String, Object> result = conversationService.getConversationByIssuePayload(12L);

        assertTrue(result.containsKey("conversation"));
        assertNull(result.get("conversation"));
    }

    @Test
    void getConversationByIssuePayload_should_include_messages_and_users() {
        Conversation conversation = Conversation.builder().id(3L).build();
        User user = new User();
        user.setId(2L);
        user.setName("Alice");
        user.setEmail("alice@test.com");
        user.setRoles(Set.of());

        Message message = Message.builder().id(9L).content("Bonjour").user(user).build();
        ConversationUser conversationUser = ConversationUser.builder()
                .role(ConversationRole.INITIATOR)
                .user(user)
                .conversation(conversation)
                .build();

        when(conversationRepository.findByIssueId(12L)).thenReturn(Optional.of(conversation));
        when(conversationRepository.findById(3L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationOrderByCreatedAtAsc(conversation)).thenReturn(List.of(message));
        when(conversationUserRepository.findByConversation(conversation)).thenReturn(List.of(conversationUser));

        Map<String, Object> result = conversationService.getConversationByIssuePayload(12L);

        Map<String, Object> conversationPayload = (Map<String, Object>) result.get("conversation");
        assertNotNull(conversationPayload);
        assertEquals(3L, conversationPayload.get("id"));
        assertEquals(1, ((List<?>) conversationPayload.get("messages")).size());
        assertEquals(1, ((List<?>) conversationPayload.get("users")).size());
    }

    @Test
    void addMessage_should_validate_user_and_broadcast_payload() {
        Conversation conversation = Conversation.builder().id(44L).build();
        Issue issue = new Issue();
        issue.setId(77L);
        User user = new User();
        user.setId(5L);
        user.setName("Bob");
        user.setEmail("bob@test.com");
        user.setRoles(Set.of());

        Message message = Message.builder()
                .id(101L)
                .content("Salut")
                .conversation(conversation)
                .issue(issue)
                .user(user)
                .build();

        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));
        when(conversationRepository.findById(44L)).thenReturn(Optional.of(conversation));
        when(issueRepository.findById(77L)).thenReturn(Optional.of(issue));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        ConversationService.MessageDispatchResult result = conversationService.addMessage(44L, Map.of("content", "Salut", "issueId", "77"), "bob@test.com");

        assertEquals(101L, result.message().get("id"));
        assertEquals("Salut", result.message().get("content"));
        assertEquals(77L, result.issueId());
        assertTrue(result.broadcastPayload().contains("new_message"));
    }

    @Test
    void addMessage_should_include_reply_to_message_when_provided() {
        Conversation conversation = Conversation.builder().id(44L).build();
        Issue issue = new Issue();
        issue.setId(77L);
        User user = new User();
        user.setId(5L);
        user.setName("Bob");
        user.setEmail("bob@test.com");
        user.setRoles(Set.of());

        Message parent = Message.builder()
                .id(55L)
                .content("Message initial")
                .conversation(conversation)
                .issue(issue)
                .user(user)
                .build();

        Message reply = Message.builder()
                .id(101L)
                .content("Réponse")
                .conversation(conversation)
                .issue(issue)
                .user(user)
                .replyTo(parent)
                .build();

        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));
        when(conversationRepository.findById(44L)).thenReturn(Optional.of(conversation));
        when(issueRepository.findById(77L)).thenReturn(Optional.of(issue));
        when(messageRepository.findById(55L)).thenReturn(Optional.of(parent));
        when(messageRepository.save(any(Message.class))).thenReturn(reply);

        ConversationService.MessageDispatchResult result = conversationService.addMessage(
                44L,
                Map.of("content", "Réponse", "issueId", "77", "replyToMessageId", "55"),
                "bob@test.com");

        assertEquals(55L, result.message().get("replyToMessageId"));
        assertNotNull(result.message().get("replyToMessage"));
        assertTrue(result.broadcastPayload().contains("\"replyToMessageId\":55"));
        assertTrue(result.broadcastPayload().contains("\"replyToMessage\""));
        verify(messageRepository).findById(55L);
    }

    @Test
    void addMessage_should_throw_when_reply_to_message_missing() {
        Conversation conversation = Conversation.builder().id(44L).build();
        Issue issue = new Issue();
        issue.setId(77L);
        User user = new User();
        user.setId(5L);
        user.setEmail("bob@test.com");

        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));
        when(conversationRepository.findById(44L)).thenReturn(Optional.of(conversation));
        when(issueRepository.findById(77L)).thenReturn(Optional.of(issue));
        when(messageRepository.findById(55L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.function.Executable executable = () -> conversationService.addMessage(
                44L,
                Map.of("content", "Réponse", "issueId", "77", "replyToMessageId", "55"),
                "bob@test.com");

        MessageNotFoundException ex = assertThrows(MessageNotFoundException.class, executable);
        assertEquals("Message not found with id: 55", ex.getMessage());
    }

    @Test
    void addMessage_should_throw_when_user_missing() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        Map<String, String> payload = Map.of("content", "Salut", "issueId", "77");

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> conversationService.addMessage(44L, payload, "missing@test.com"));

        assertEquals("User not found with email: missing@test.com", ex.getMessage());
    }

    @Test
    void addMessage_should_throw_when_content_is_too_long() {
        User user = new User();
        user.setId(5L);
        user.setEmail("bob@test.com");

        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));

        String longContent = "a".repeat(4001);
        Map<String, String> payload = Map.of("content", longContent, "issueId", "77");
        MessageTooLongException ex = assertThrows(MessageTooLongException.class,
                () -> conversationService.addMessage(44L, payload, "bob@test.com"));

        assertEquals("Le message dépasse la limite de 4000 caractères", ex.getMessage());
    }

    @Test
    void addMessage_should_throw_when_issueId_is_missing() {
        User user = new User();
        user.setId(5L);
        user.setEmail("bob@test.com");

        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));

        Map<String, String> payload = Map.of("content", "Test message");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> conversationService.addMessage(44L, payload, "bob@test.com"));

        assertEquals("Missing issue id in payload", ex.getMessage());
    }

    @Test
    void addMessage_should_throw_when_issueId_is_blank() {
        User user = new User();
        user.setId(5L);
        user.setEmail("bob@test.com");

        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));

        Map<String, String> payload = Map.of("content", "Test message", "issueId", "  ");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> conversationService.addMessage(44L, payload, "bob@test.com"));

        assertEquals("Missing issue id in payload", ex.getMessage());
    }

    @Test
    void addMessage_should_throw_when_issueId_is_invalid() {
        User user = new User();
        user.setId(5L);
        user.setEmail("bob@test.com");

        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));

        Map<String, String> payload = Map.of("content", "Test message", "issueId", "invalid-number");
        NumberFormatException ex = assertThrows(NumberFormatException.class,
                () -> conversationService.addMessage(44L, payload, "bob@test.com"));

        assertNotNull(ex);
    }

    @Test
    void addMessageToConversation_should_throw_when_reply_target_not_in_same_conversation() {
        Conversation conversation = Conversation.builder().id(44L).build();
        Conversation otherConversation = Conversation.builder().id(99L).build();
        Issue issue = new Issue();
        issue.setId(77L);
        User user = new User();
        user.setId(5L);

        Message replyTo = Message.builder()
                .id(55L)
                .content("Message initial")
                .conversation(otherConversation)
                .issue(issue)
                .user(user)
                .build();

        when(conversationRepository.findById(44L)).thenReturn(Optional.of(conversation));
        when(issueRepository.findById(77L)).thenReturn(Optional.of(issue));
        when(messageRepository.findById(55L)).thenReturn(Optional.of(replyTo));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> conversationService.addMessageToConversation(44L, "Réponse", user, 77L, 55L));

        assertEquals("Reply target must belong to the same conversation", ex.getMessage());
    }

    @Test
    void addMessageToConversation_should_throw_when_reply_to_has_null_conversation() {
        Conversation conversation = Conversation.builder().id(44L).build();
        Issue issue = new Issue();
        issue.setId(77L);
        User user = new User();
        user.setId(5L);

        Message replyTo = Message.builder()
                .id(55L)
                .content("Message initial")
                .conversation(null)
                .issue(issue)
                .user(user)
                .build();

        when(conversationRepository.findById(44L)).thenReturn(Optional.of(conversation));
        when(issueRepository.findById(77L)).thenReturn(Optional.of(issue));
        when(messageRepository.findById(55L)).thenReturn(Optional.of(replyTo));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> conversationService.addMessageToConversation(44L, "Réponse", user, 77L, 55L));

        assertEquals("Reply target must belong to the same conversation", ex.getMessage());
    }

    @Test
    void getOrCreateConversationForIssue_should_create_conversation_when_missing() {
        Issue issue = new Issue();
        issue.setId(5L);

        Conversation newConversation = Conversation.builder().id(100L).issue(issue).build();

        when(conversationRepository.findByIssueId(5L)).thenReturn(Optional.empty());
        when(issueRepository.findById(5L)).thenReturn(Optional.of(issue));
        when(conversationRepository.save(org.mockito.ArgumentMatchers.any(Conversation.class))).thenReturn(newConversation);
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(newConversation));
        when(messageRepository.findByConversationOrderByCreatedAtAsc(newConversation)).thenReturn(List.of());
        when(conversationUserRepository.findByConversation(newConversation)).thenReturn(List.of());

        Map<String, Object> result = conversationService.getOrCreateConversationForIssue(5L);

        assertNotNull(result);
        assertTrue(result.containsKey("conversation"));
        assertNotNull(result.get("conversation"));
        assertEquals(100L, ((Map<String, Object>) result.get("conversation")).get("id"));
    }

    @Test
    void getOrCreateConversationForIssue_should_throw_when_issue_not_found() {
        when(conversationRepository.findByIssueId(5L)).thenReturn(Optional.empty());
        when(issueRepository.findById(5L)).thenReturn(Optional.empty());

        IssueNotFoundException ex = assertThrows(IssueNotFoundException.class,
                () -> conversationService.getOrCreateConversationForIssue(5L));

        assertEquals("Issue not found with id: 5", ex.getMessage());
    }

    @Test
    void addMessage_should_accept_max_length_message() {
        Conversation conversation = Conversation.builder().id(44L).build();
        Issue issue = new Issue();
        issue.setId(77L);
        User user = new User();
        user.setId(5L);
        user.setEmail("bob@test.com");
        user.setRoles(Set.of());

        // Message with exactly 4000 characters should be accepted
        String maxContent = "a".repeat(4000);
        Message message = Message.builder()
                .id(101L)
                .content(maxContent)
                .conversation(conversation)
                .issue(issue)
                .user(user)
                .build();

        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));
        when(conversationRepository.findById(44L)).thenReturn(Optional.of(conversation));
        when(issueRepository.findById(77L)).thenReturn(Optional.of(issue));
        when(messageRepository.save(org.mockito.ArgumentMatchers.any(Message.class))).thenReturn(message);

        ConversationService.MessageDispatchResult result = conversationService.addMessage(44L,
                Map.of("content", maxContent, "issueId", "77"), "bob@test.com");

        assertNotNull(result);
        assertEquals(maxContent, result.message().get("content"));
    }

    @Test
    void addMessage_should_accept_null_content() {
        Conversation conversation = Conversation.builder().id(44L).build();
        Issue issue = new Issue();
        issue.setId(77L);
        User user = new User();
        user.setId(5L);
        user.setEmail("bob@test.com");
        user.setRoles(Set.of());

        Message message = Message.builder()
                .id(101L)
                .content(null)
                .conversation(conversation)
                .issue(issue)
                .user(user)
                .build();

        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));
        when(conversationRepository.findById(44L)).thenReturn(Optional.of(conversation));
        when(issueRepository.findById(77L)).thenReturn(Optional.of(issue));
        when(messageRepository.save(org.mockito.ArgumentMatchers.any(Message.class))).thenReturn(message);

        ConversationService.MessageDispatchResult result = conversationService.addMessage(44L,
                Map.of("issueId", "77"), "bob@test.com");

        assertNotNull(result);
        assertNull(result.message().get("content"));
    }
}
