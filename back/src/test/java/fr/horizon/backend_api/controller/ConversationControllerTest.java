package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.Conversation;
import fr.horizon.backend_api.model.ConversationUser;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.Message;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.ConversationRole;
import fr.horizon.backend_api.repository.UserRepository;
import fr.horizon.backend_api.service.JwtToken;
import fr.horizon.backend_api.service.ConversationService;
import fr.horizon.backend_api.websocket.IssueWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private IssueWebSocketHandler issueWebSocketHandler;

    @MockitoBean
    private JwtToken jwtToken;

    @Test
    void shouldReturnEmptyConversationWhenNotFound() throws Exception {
        when(conversationService.getConversationByIssueId(12L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/conversations/issue/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversation").value(nullValue()));

        verify(conversationService).getConversationByIssueId(12L);
    }

    @Test
    void shouldReturnConversationWithMessagesAndUsers() throws Exception {
        Conversation conversation = Conversation.builder().id(3L).build();

        Message message = Message.builder()
                .id(9L)
                .content("Bonjour")
                .build();
        User messageUser = new User();
        messageUser.setId(2L);
        messageUser.setName("Alice");
        messageUser.setEmail("alice@test.com");
        messageUser.setRoles(Set.of());
        message.setUser(messageUser);

        ConversationUser conversationUser = ConversationUser.builder()
                .role(ConversationRole.INITIATOR)
                .user(messageUser)
                .conversation(conversation)
                .build();

        when(conversationService.getConversationByIssueId(12L)).thenReturn(Optional.of(conversation));
        when(conversationService.getMessagesForConversation(3L)).thenReturn(List.of(message));
        when(conversationService.getUsersInConversation(3L)).thenReturn(List.of(conversationUser));

        mockMvc.perform(get("/conversations/issue/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversation.id").value(3))
                .andExpect(jsonPath("$.conversation.messages[0].id").value(9))
                .andExpect(jsonPath("$.conversation.messages[0].content").value("Bonjour"))
                .andExpect(jsonPath("$.conversation.messages[0].user.email").value("alice@test.com"))
                .andExpect(jsonPath("$.conversation.users[0].conversationRole").value("INITIATOR"));

        verify(conversationService).getMessagesForConversation(3L);
        verify(conversationService).getUsersInConversation(3L);
    }

    @Test
    void shouldAddMessageAndBroadcastToWebSocket() throws Exception {
        Conversation conversation = Conversation.builder().id(44L).build();
        Issue issue = new Issue();
        issue.setId(77L);
        User user = new User();
        user.setId(5L);
        user.setName("Bob");
        user.setEmail("bob@test.com");

        Message message = Message.builder()
                .id(101L)
                .content("Salut")
                .conversation(conversation)
                .issue(issue)
                .user(user)
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("bob@test.com");
        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));
        when(conversationService.addMessageToConversation(eq(44L), eq("Salut"), eq(user), eq(77L)))
                .thenReturn(message);

        mockMvc.perform(post("/conversations/44/messages")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Salut",
                                  "issueId": "77"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.content").value("Salut"))
                .andExpect(jsonPath("$.user.email").value("bob@test.com"));

        verify(conversationService).addMessageToConversation(44L, "Salut", user, 77L);
        verify(issueWebSocketHandler).broadcastToIssue(eq(77L), any(String.class));
    }
}


