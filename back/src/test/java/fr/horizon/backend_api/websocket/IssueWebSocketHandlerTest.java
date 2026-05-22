package fr.horizon.backend_api.websocket;

import fr.horizon.backend_api.model.Message;
import fr.horizon.backend_api.model.Conversation;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.repository.UserRepository;
import fr.horizon.backend_api.service.ConversationService;
import fr.horizon.backend_api.service.JwtToken;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueWebSocketHandlerTest {

    @Mock
    private JwtToken jwtToken;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private IssueWebSocketHandler issueWebSocketHandler;

    @BeforeEach
    void clearSessions() throws Exception {
        getSessions().clear();
    }

    @Test
    void afterConnectionEstablished_shouldCloseBadData_whenTokenIsMissing() throws Exception {
        WebSocketSession session = mockSession();
        withQuery(session, "issueId=12");

        issueWebSocketHandler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
        verify(jwtToken, never()).parseToken(any());
    }

    @Test
    void afterConnectionEstablished_shouldCloseBadData_whenIssueIdIsInvalid() throws Exception {
        WebSocketSession session = mockSession();
        withQuery(session, "token=abc&issueId=notANumber");

        issueWebSocketHandler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
        verify(jwtToken, never()).parseToken(any());
    }

    @Test
    void afterConnectionEstablished_shouldCloseNotAcceptable_whenTokenParsingFails() throws Exception {
        WebSocketSession session = mockSession();
        withQuery(session, "token=abc&issueId=12");
        when(jwtToken.parseToken("abc")).thenThrow(new RuntimeException("invalid token"));

        issueWebSocketHandler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.NOT_ACCEPTABLE);
    }

    @Test
    void afterConnectionEstablished_shouldCloseBadData_whenUserNotFound() throws Exception {
        WebSocketSession session = mockSession();
        withQuery(session, "token=abc&issueId=12");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("missing@test.com");
        when(jwtToken.parseToken("abc")).thenReturn(claims);
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        issueWebSocketHandler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void afterConnectionEstablished_shouldStoreAttributesAndRegisterSession_whenAuthenticationSucceeds() throws Exception {
        WebSocketSession session = mockSession();
        withQuery(session, "token=abc&issueId=12");
        withOpen(session);
        Claims claims = mock(Claims.class);
        User user = User.builder().id(7L).email("john@test.com").name("John").build();

        when(claims.getSubject()).thenReturn("john@test.com");
        when(jwtToken.parseToken("abc")).thenReturn(claims);
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));

        issueWebSocketHandler.afterConnectionEstablished(session);

        Map<String, Object> attributes = session.getAttributes();
        assertEquals("john@test.com", attributes.get("userEmail"));
        assertEquals(12L, attributes.get("issueId"));
        assertEquals(7L, attributes.get("userId"));

        issueWebSocketHandler.broadcastToIssue(12L, "ping");
        verify(session).sendMessage(argThat(msg -> "ping".equals(msg.getPayload())));
    }

    @Test
    void handleTextMessage_shouldIgnoreNonMessageType() throws Exception {
        WebSocketSession session = mockSession();

        issueWebSocketHandler.handleTextMessage(session, new TextMessage("{\"type\":\"ping\"}"));

        verify(conversationService, never()).addMessageToConversation(any(), any(), any(), any(), any());
    }

    @Test
    void handleTextMessage_shouldReturnWithoutPersisting_whenContentIsEmpty() throws Exception {
        WebSocketSession session = mockSession();
        session.getAttributes().put("issueId", 12L);
        session.getAttributes().put("userEmail", "john@test.com");

        issueWebSocketHandler.handleTextMessage(session,
                new TextMessage("{\"type\":\"message\",\"content\":\"\",\"conversationId\":5}"));

        verify(userRepository, never()).findByEmail(any());
        verify(conversationService, never()).addMessageToConversation(any(), any(), any(), any(), any());
    }

    @Test
    void handleTextMessage_shouldReturnWithoutPersisting_whenConversationIdIsZero() throws Exception {
        WebSocketSession session = mockSession();
        session.getAttributes().put("issueId", 12L);
        session.getAttributes().put("userEmail", "john@test.com");

        issueWebSocketHandler.handleTextMessage(session,
                new TextMessage("{\"type\":\"message\",\"content\":\"Bonjour\",\"conversationId\":0}"));

        verify(userRepository, never()).findByEmail(any());
        verify(conversationService, never()).addMessageToConversation(any(), any(), any(), any(), any());
    }

    @Test
    void handleTextMessage_shouldReturnWithoutPersisting_whenUserIsUnknown() throws Exception {
        WebSocketSession session = mockSession();
        session.getAttributes().put("issueId", 12L);
        session.getAttributes().put("userEmail", "missing@test.com");
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        issueWebSocketHandler.handleTextMessage(session,
                new TextMessage("{\"type\":\"message\",\"content\":\"Bonjour\",\"conversationId\":5}"));

        verify(conversationService, never()).addMessageToConversation(any(), any(), any(), any(), any());
    }

    @Test
    void handleTextMessage_shouldPersistAndBroadcast_whenPayloadIsValid() throws Exception {
        WebSocketSession session = mockSession();
        withOpen(session);
        session.getAttributes().put("issueId", 12L);
        session.getAttributes().put("userEmail", "john@test.com");
        addSession(session);

        User user = User.builder()
                .id(9L)
                .name("John")
                .email("john@test.com")
                .roles(Set.of(new Role("ADMIN")))
                .build();
        Message message = Message.builder()
                .id(42L)
                .content("Bonjour")
                .build();

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(conversationService.addMessageToConversation(5L, "Bonjour", user, 12L, null)).thenReturn(message);
        when(conversationService.buildBroadcastPayload(message, 12L)).thenReturn("{\"type\":\"new_message\",\"issueId\":12,\"message\":{\"id\":42,\"user\":{\"displayRole\":\"ADMIN\"}}}");

        issueWebSocketHandler.handleTextMessage(session,
                new TextMessage("{\"type\":\"message\",\"content\":\"Bonjour\",\"conversationId\":5}"));

        verify(conversationService).addMessageToConversation(5L, "Bonjour", user, 12L, null);

        ArgumentCaptor<TextMessage> payloadCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(payloadCaptor.capture());

        String payload = payloadCaptor.getValue().getPayload();
        assertTrue(payload.contains("\"type\":\"new_message\""));
        assertTrue(payload.contains("\"displayRole\":\"ADMIN\""));
        assertTrue(payload.contains("\"issueId\":12"));
    }

    @Test
    void handleTextMessage_shouldPersistReplyAndBroadcast_whenReplyTargetIsProvided() throws Exception {
        WebSocketSession session = mockSession();
        withOpen(session);
        session.getAttributes().put("issueId", 12L);
        session.getAttributes().put("userEmail", "john@test.com");
        addSession(session);

        User user = User.builder()
                .id(9L)
                .name("John")
                .email("john@test.com")
                .roles(Set.of(new Role("ADMIN")))
                .build();
        Conversation conversation = fr.horizon.backend_api.model.Conversation.builder().id(5L).build();
        Message parent = Message.builder().id(15L).conversation(conversation).build();
        Message message = Message.builder()
                .id(42L)
                .content("Réponse")
                .replyTo(parent)
                .build();

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(conversationService.addMessageToConversation(5L, "Réponse", user, 12L, 15L)).thenReturn(message);
        when(conversationService.buildBroadcastPayload(message, 12L)).thenReturn("{\"type\":\"new_message\",\"issueId\":12,\"message\":{\"id\":42,\"replyToMessageId\":15,\"replyToMessage\":{\"id\":15}}}");

        issueWebSocketHandler.handleTextMessage(session,
                new TextMessage("{\"type\":\"message\",\"content\":\"Réponse\",\"conversationId\":5,\"replyToMessageId\":15}"));

        verify(conversationService).addMessageToConversation(5L, "Réponse", user, 12L, 15L);

        ArgumentCaptor<TextMessage> payloadCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(payloadCaptor.capture());

        String payload = payloadCaptor.getValue().getPayload();
        assertTrue(payload.contains("\"replyToMessageId\":15"));
        assertTrue(payload.contains("\"replyToMessage\""));
    }

    @Test
    void broadcastToIssue_shouldNotThrow_whenSendFails() throws Exception {
        WebSocketSession session = mockSession();
        withOpen(session);
        session.getAttributes().put("issueId", 99L);
        addSession(session);
        doThrow(new IOException("network error")).when(session).sendMessage(any(TextMessage.class));

        assertDoesNotThrow(() -> issueWebSocketHandler.broadcastToIssue(99L, "hello"));
    }

    @Test
    void afterConnectionClosed_shouldRemoveSessionFromBroadcastList() throws Exception {
        WebSocketSession session = mockSession();
        withOpen(session);
        session.getAttributes().put("issueId", 12L);
        addSession(session);

        issueWebSocketHandler.afterConnectionClosed(session, CloseStatus.NORMAL);
        issueWebSocketHandler.broadcastToIssue(12L, "message");

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    private WebSocketSession mockSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();

        lenient().when(session.getAttributes()).thenReturn(attributes);
        return session;
    }

    private void withQuery(WebSocketSession session, String queryString) {
        URI uri = URI.create("ws://localhost/ws/issue?" + queryString);
        when(session.getUri()).thenReturn(uri);
    }

    private void withOpen(WebSocketSession session) {
        lenient().when(session.isOpen()).thenReturn(true);
    }

    @SuppressWarnings("unchecked")
    private List<WebSocketSession> getSessions() throws Exception {
        Field field = IssueWebSocketHandler.class.getDeclaredField("sessions");
        field.setAccessible(true);
        return (List<WebSocketSession>) field.get(null);
    }

    private void addSession(WebSocketSession session) throws Exception {
        getSessions().add(session);
    }
}

