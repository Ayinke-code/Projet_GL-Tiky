package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.ConversationNotFoundException;
import fr.horizon.backend_api.exception.IssueNotFoundException;
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

        ConversationNotFoundException ex = assertThrows(ConversationNotFoundException.class,
                () -> conversationService.addMessageToConversation(11L, "Hello", new User(), 22L));

        assertEquals("Conversation not found with id: 11", ex.getMessage());
    }

    @Test
    void addMessageToConversation_should_throw_when_issue_missing() {
        Conversation conversation = Conversation.builder().id(11L).build();
        when(conversationRepository.findById(11L)).thenReturn(Optional.of(conversation));
        when(issueRepository.findById(22L)).thenReturn(Optional.empty());

        IssueNotFoundException ex = assertThrows(IssueNotFoundException.class,
                () -> conversationService.addMessageToConversation(11L, "Hello", new User(), 22L));

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
    void addMessage_should_throw_when_user_missing() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> conversationService.addMessage(44L, Map.of("content", "Salut", "issueId", "77"), "missing@test.com"));

        assertEquals("User not found with email: missing@test.com", ex.getMessage());
    }

    @Test
    void addMessage_should_throw_when_content_is_too_long() {
        User user = new User();
        user.setId(5L);
        user.setEmail("bob@test.com");

        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));

        String longContent = "a".repeat(4001);
        MessageTooLongException ex = assertThrows(MessageTooLongException.class,
                () -> conversationService.addMessage(44L, Map.of("content", longContent, "issueId", "77"), "bob@test.com"));

        assertEquals("Le message dépasse la limite de 4000 caractères", ex.getMessage());
    }
}

