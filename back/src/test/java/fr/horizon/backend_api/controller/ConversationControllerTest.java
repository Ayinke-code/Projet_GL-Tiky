package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.service.ConversationService;
import fr.horizon.backend_api.service.JwtToken;
import fr.horizon.backend_api.websocket.IssueWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

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
    private JwtToken jwtToken;

    @MockitoBean
    private IssueWebSocketHandler issueWebSocketHandler;

    @Test
    void shouldReturnEmptyConversationWhenNotFound() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversation", null);
        when(conversationService.getConversationByIssuePayload(12L)).thenReturn(payload);

        mockMvc.perform(get("/conversations/issue/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversation").value(nullValue()));

        verify(conversationService).getConversationByIssuePayload(12L);
    }

    @Test
    void shouldReturnConversationWithMessagesAndUsers() throws Exception {
        Map<String, Object> user = new HashMap<>();
        user.put("id", 2L);
        user.put("name", "Alice");
        user.put("email", "alice@test.com");
        user.put("role", "STUDENT");
        user.put("roles", java.util.List.of("STUDENT"));

        Map<String, Object> message = new HashMap<>();
        message.put("id", 9L);
        message.put("content", "Bonjour");
        message.put("user", user);

        Map<String, Object> conversation = new HashMap<>();
        conversation.put("id", 3L);
        conversation.put("messages", java.util.List.of(message));
        conversation.put("users", java.util.List.of(Map.of(
                "id", 2L,
                "name", "Alice",
                "email", "alice@test.com",
                "role", "STUDENT",
                "roles", java.util.List.of("STUDENT"),
                "conversationRole", "INITIATOR"
        )));

        Map<String, Object> payload = new HashMap<>();
        payload.put("conversation", conversation);

        when(conversationService.getConversationByIssuePayload(12L)).thenReturn(payload);

        mockMvc.perform(get("/conversations/issue/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversation.id").value(3))
                .andExpect(jsonPath("$.conversation.messages[0].id").value(9))
                .andExpect(jsonPath("$.conversation.messages[0].content").value("Bonjour"))
                .andExpect(jsonPath("$.conversation.messages[0].user.email").value("alice@test.com"))
                .andExpect(jsonPath("$.conversation.users[0].conversationRole").value("INITIATOR"));

        verify(conversationService).getConversationByIssuePayload(12L);
    }

    @Test
    void shouldAddMessageAndBroadcastToWebSocket() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("bob@test.com");

        Map<String, Object> response = new HashMap<>();
        response.put("id", 101L);
        response.put("content", "Salut");
        response.put("createdAt", null);
        response.put("user", Map.of(
                "id", 5L,
                "name", "Bob",
                "email", "bob@test.com",
                "role", "STUDENT",
                "roles", java.util.List.of("STUDENT")
        ));

        when(conversationService.addMessage(44L, Map.of("content", "Salut", "issueId", "77"), "bob@test.com"))
                .thenReturn(new ConversationService.MessageDispatchResult(response, "{\"type\":\"new_message\"}", 77L));

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

        verify(conversationService).addMessage(44L, Map.of("content", "Salut", "issueId", "77"), "bob@test.com");
        verify(issueWebSocketHandler).broadcastToIssue(77L, "{\"type\":\"new_message\"}");
    }
}


