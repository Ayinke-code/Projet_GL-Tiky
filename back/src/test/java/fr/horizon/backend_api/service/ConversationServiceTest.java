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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> conversationService.addMessageToConversation(11L, "Hello", new User(), 22L));

        assertEquals("Conversation not found", ex.getMessage());
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
}

