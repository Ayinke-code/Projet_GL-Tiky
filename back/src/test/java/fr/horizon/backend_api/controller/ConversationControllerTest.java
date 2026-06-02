package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.service.ConversationService;
import fr.horizon.backend_api.service.JwtToken;
import fr.horizon.backend_api.websocket.ConversationWebSocketHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
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
    private ConversationWebSocketHandler conversationWebSocketHandler;

    // Test constants for better readability
    private static final Long ISSUE_ID = 12L;
    private static final Long CONVERSATION_ID = 44L;
    private static final Long MESSAGE_ID = 101L;
    private static final Long USER_ID = 5L;
    private static final Long REPLY_MESSAGE_ID = 55L;
    private static final String USER_EMAIL = "bob@test.com";
    private static final String MESSAGE_CONTENT = "Salut";
    private static final String BROADCAST_PAYLOAD = "{\"type\":\"new_message\"}";

    @Test
    void shouldInitializeConversationForIssue() throws Exception {
        Map<String, Object> conversation = new HashMap<>();
        conversation.put("id", 3L);
        conversation.put("messages", java.util.List.of());
        conversation.put("users", java.util.List.of());

        Map<String, Object> payload = new HashMap<>();
        payload.put("conversation", conversation);

        when(conversationService.getOrCreateConversationForIssue(ISSUE_ID)).thenReturn(payload);

        mockMvc.perform(post("/conversations/issue/" + ISSUE_ID + "/initialize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversation.id").value(3));

        verify(conversationService).getOrCreateConversationForIssue(ISSUE_ID);
    }

    @Test
    void shouldReturnEmptyConversationWhenNotFound() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversation", null);
        when(conversationService.getConversationByIssuePayload(ISSUE_ID)).thenReturn(payload);

        mockMvc.perform(get("/conversations/issue/" + ISSUE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversation").value(nullValue()));

        verify(conversationService).getConversationByIssuePayload(ISSUE_ID);
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

        when(conversationService.getConversationByIssuePayload(ISSUE_ID)).thenReturn(payload);

        mockMvc.perform(get("/conversations/issue/" + ISSUE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversation.id").value(3))
                .andExpect(jsonPath("$.conversation.messages[0].id").value(9))
                .andExpect(jsonPath("$.conversation.messages[0].content").value("Bonjour"))
                .andExpect(jsonPath("$.conversation.messages[0].user.email").value("alice@test.com"))
                .andExpect(jsonPath("$.conversation.users[0].conversationRole").value("INITIATOR"));

        verify(conversationService).getConversationByIssuePayload(ISSUE_ID);
    }

    @Test
    void shouldAddMessageAndBroadcastToWebSocket() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(USER_EMAIL);

        Map<String, Object> response = new HashMap<>();
        response.put("id", MESSAGE_ID);
        response.put("content", MESSAGE_CONTENT);
        response.put("createdAt", null);
        response.put("user", Map.of(
                "id", USER_ID,
                "name", "Bob",
                "email", USER_EMAIL,
                "role", "STUDENT",
                "roles", java.util.List.of("STUDENT")
        ));

        when(conversationService.addMessage(CONVERSATION_ID, Map.of("content", MESSAGE_CONTENT, "issueId", "77"), USER_EMAIL))
                .thenReturn(new ConversationService.MessageDispatchResult(response, BROADCAST_PAYLOAD, 77L));

        mockMvc.perform(post("/conversations/" + CONVERSATION_ID + "/messages")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Salut",
                                  "issueId": "77"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(MESSAGE_ID))
                .andExpect(jsonPath("$.content").value(MESSAGE_CONTENT))
                .andExpect(jsonPath("$.user.email").value(USER_EMAIL));

        verify(conversationService).addMessage(CONVERSATION_ID, Map.of("content", MESSAGE_CONTENT, "issueId", "77"), USER_EMAIL);
        verify(conversationWebSocketHandler).broadcastToConversation(CONVERSATION_ID, BROADCAST_PAYLOAD);
    }

    @Test
    void shouldReturnCreatedEvenIfBroadcastFails() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(USER_EMAIL);

        Map<String, Object> response = new HashMap<>();
        response.put("id", MESSAGE_ID);
        response.put("content", MESSAGE_CONTENT);
        response.put("user", Map.of("id", USER_ID, "email", USER_EMAIL));

        when(conversationService.addMessage(anyLong(), any(), anyString()))
                .thenReturn(new ConversationService.MessageDispatchResult(response, BROADCAST_PAYLOAD, 77L));

        // Simulate WebSocket broadcast failure
        doThrow(new RuntimeException("WebSocket broadcast failed"))
                .when(conversationWebSocketHandler)
                .broadcastToConversation(anyLong(), anyString());

        mockMvc.perform(post("/conversations/" + CONVERSATION_ID + "/messages")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Salut",
                                  "issueId": "77"
                                }
                                """))
                // HTTP response should still be created (201)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(MESSAGE_ID))
                .andExpect(jsonPath("$.content").value(MESSAGE_CONTENT));

        // Verify broadcast was attempted despite failure
        verify(conversationWebSocketHandler).broadcastToConversation(anyLong(), anyString());
    }

    @Test
    void shouldAddMessageWithReplyReference() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(USER_EMAIL);

        Map<String, Object> response = new HashMap<>();
        response.put("id", MESSAGE_ID);
        response.put("content", "Réponse");
        response.put("replyToMessageId", REPLY_MESSAGE_ID);
        response.put("user", Map.of("id", USER_ID, "email", USER_EMAIL));

        when(conversationService.addMessage(CONVERSATION_ID,
                Map.of("content", "Réponse", "issueId", "77", "replyToMessageId", "55"),
                USER_EMAIL))
                .thenReturn(new ConversationService.MessageDispatchResult(response, BROADCAST_PAYLOAD, 77L));

        mockMvc.perform(post("/conversations/" + CONVERSATION_ID + "/messages")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Réponse",
                                  "issueId": "77",
                                  "replyToMessageId": "55"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(MESSAGE_ID))
                .andExpect(jsonPath("$.replyToMessageId").value(REPLY_MESSAGE_ID));

        verify(conversationService).addMessage(CONVERSATION_ID,
                Map.of("content", "Réponse", "issueId", "77", "replyToMessageId", "55"),
                USER_EMAIL);
    }
}


