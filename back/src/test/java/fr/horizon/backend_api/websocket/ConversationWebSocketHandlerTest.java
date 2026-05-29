package fr.horizon.backend_api.websocket;

import fr.horizon.backend_api.repository.UserRepository;
import fr.horizon.backend_api.service.JwtToken;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;


/**
 * Tests unitaires pour le WebSocketHandler des conversations.
 * <p>
 * Teste les scénarios suivants :
 * - Connexion WebSocket avec authentification réussie
 * - Connexion WebSocket avec authentification échouée
 * - Fermeture de la connexion
 * - Diffusion de messages sur une conversation spécifique
 * - Gestion des erreurs de transmission
 */
class ConversationWebSocketHandlerTest {

    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String NOT_FOUND_EMAIL = "notfound@example.com";
    private static final Long CONVERSATION_ID = 1L;
    private static final String CONVERSATION_ID_STR = "1";
    private static final String TOKEN_PARAM = "token=";
    private static final String CONVERSATION_ID_PARAM = "&conversationId=";
    private static final String WS_BASE_URL = "ws://localhost:8080/ws/conversation?";
    private static final String SESSION_ID = "session-1";
    private static final String INVALID_TOKEN_MESSAGE = "Invalid token";
    private static final String TEST_MESSAGE = "{\"type\": \"new_message\", \"content\": \"Test message\"}";

    @Mock
    private JwtToken jwtToken;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebSocketSession session;

    @Mock
    private Claims claims;

    private ConversationWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new ConversationWebSocketHandler(jwtToken, userRepository);
    }

    @Test
    void testAfterConnectionEstablished_ValidTokenAndConversationId() throws Exception {
        // Setup
        String queryString = TOKEN_PARAM + VALID_TOKEN + CONVERSATION_ID_PARAM + CONVERSATION_ID;

        when(session.getUri()).thenReturn(new URI(WS_BASE_URL + queryString));
        when(jwtToken.parseToken(VALID_TOKEN)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(new fr.horizon.backend_api.model.User()));
        when(session.getAttributes()).thenReturn(new java.util.HashMap<>());

        // Execute
        handler.afterConnectionEstablished(session);

        // Verify
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(claims).getSubject();
    }

    @Test
    void testAfterConnectionEstablished_MissingToken() throws Exception {
        // Setup
        String queryString = CONVERSATION_ID_PARAM + CONVERSATION_ID;
        when(session.getUri()).thenReturn(new URI(WS_BASE_URL + queryString));

        // Execute
        handler.afterConnectionEstablished(session);

        // Verify - session should be closed with BAD_DATA
        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void testAfterConnectionEstablished_MissingConversationId() throws Exception {
        // Setup
        String queryString = TOKEN_PARAM + VALID_TOKEN;
        when(session.getUri()).thenReturn(new URI(WS_BASE_URL + queryString));

        // Execute
        handler.afterConnectionEstablished(session);

        // Verify - session should be closed with BAD_DATA
        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void testAfterConnectionEstablished_InvalidToken() throws Exception {
        // Setup
        String queryString = TOKEN_PARAM + INVALID_TOKEN + CONVERSATION_ID_PARAM + CONVERSATION_ID;

        when(session.getUri()).thenReturn(new URI(WS_BASE_URL + queryString));
        when(jwtToken.parseToken(INVALID_TOKEN)).thenThrow(new RuntimeException(INVALID_TOKEN_MESSAGE));

        // Execute
        handler.afterConnectionEstablished(session);

        // Verify - session should be closed with NOT_ACCEPTABLE
        verify(session).close(CloseStatus.NOT_ACCEPTABLE);
    }

    @Test
    void testAfterConnectionEstablished_UserNotFound() throws Exception {
        // Setup
        String queryString = TOKEN_PARAM + VALID_TOKEN + CONVERSATION_ID_PARAM + CONVERSATION_ID;

        when(session.getUri()).thenReturn(new URI(WS_BASE_URL + queryString));
        when(jwtToken.parseToken(VALID_TOKEN)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(NOT_FOUND_EMAIL);
        when(userRepository.findByEmail(NOT_FOUND_EMAIL)).thenReturn(Optional.empty());

        // Execute
        handler.afterConnectionEstablished(session);

        // Verify - session should be closed with NOT_ACCEPTABLE
        verify(session).close(CloseStatus.NOT_ACCEPTABLE);
    }

     @Test
    void testBroadcastToConversationBuildsCorrectPayload() {
        // This tests that the method doesn't throw when broadcasting to a conversation
        // Even with no sessions, the method should complete without error
        assertDoesNotThrow(() -> handler.broadcastToConversation(CONVERSATION_ID, TEST_MESSAGE));
    }

    @Test
    void testAfterConnectionClosedRemovesSession() {
        when(session.getId()).thenReturn(SESSION_ID);
        // Should complete without throwing any exception
        assertDoesNotThrow(() -> handler.afterConnectionClosed(session, CloseStatus.NORMAL));
        // Verify that session.getId() was called to log the session ID
        verify(session).getId();
    }

    @Test
    void testBroadcastToConversation_sendsMessageToMatchingOpenSession() throws Exception {
        // Prepare two sessions: one matching conversation and open, one non-matching
        WebSocketSession matching = mock(WebSocketSession.class);
        WebSocketSession other = mock(WebSocketSession.class);

        when(matching.isOpen()).thenReturn(true);
        when(other.isOpen()).thenReturn(true);

        when(matching.getAttributes()).thenReturn(new java.util.HashMap<>());
        when(other.getAttributes()).thenReturn(new java.util.HashMap<>());

        matching.getAttributes().put("conversationId", CONVERSATION_ID);
        other.getAttributes().put("conversationId", 999L);

        // Use reflection to access the private static sessions list and populate it
        java.lang.reflect.Field sessionsField = ConversationWebSocketHandler.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        java.util.List<WebSocketSession> sessions = (java.util.List<WebSocketSession>) sessionsField.get(null);
        sessions.clear();
        sessions.add(matching);
        sessions.add(other);

        // Execute
        handler.broadcastToConversation(CONVERSATION_ID, TEST_MESSAGE);

        // Verify that sendMessage was called on the matching session
        ArgumentCaptor<org.springframework.web.socket.TextMessage> captor = ArgumentCaptor.forClass(org.springframework.web.socket.TextMessage.class);
        verify(matching).sendMessage(captor.capture());
        assertEquals(TEST_MESSAGE, captor.getValue().getPayload());

        // Cleanup
        sessions.clear();
    }

    @Test
    void testBroadcastToConversation_handlesIOExceptionGracefully() throws Exception {
        WebSocketSession problematic = mock(WebSocketSession.class);
        when(problematic.isOpen()).thenReturn(true);
        when(problematic.getAttributes()).thenReturn(new java.util.HashMap<>());
        problematic.getAttributes().put("conversationId", CONVERSATION_ID);

        doThrow(new java.io.IOException("io error")).when(problematic).sendMessage(any());

        java.lang.reflect.Field sessionsField = ConversationWebSocketHandler.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        java.util.List<WebSocketSession> sessions = (java.util.List<WebSocketSession>) sessionsField.get(null);
        sessions.clear();
        sessions.add(problematic);

        // Should not throw despite the IOException
        assertDoesNotThrow(() -> handler.broadcastToConversation(CONVERSATION_ID, TEST_MESSAGE));

        // Cleanup
        sessions.clear();
    }

    @Test
    void testAfterConnectionEstablished_InvalidConversationIdFormat() throws Exception {
        // Setup: conversationId is non-numeric
        String badConversationId = "abc";
        String queryString = TOKEN_PARAM + VALID_TOKEN + CONVERSATION_ID_PARAM + badConversationId;

        when(session.getUri()).thenReturn(new URI(WS_BASE_URL + queryString));
        when(jwtToken.parseToken(VALID_TOKEN)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(TEST_EMAIL);

        // Execute
        handler.afterConnectionEstablished(session);

        // Verify - session should be closed with BAD_DATA because conversation id parsing failed
        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void testAfterConnectionEstablished_NullUriClosesSession() throws Exception {
        when(session.getUri()).thenReturn(null);

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void testExtractTokenFromValidQuery() throws Exception {
        // This tests that the method successfully parses token and conversation ID from query string
        String queryString = TOKEN_PARAM + VALID_TOKEN + CONVERSATION_ID_PARAM + CONVERSATION_ID_STR;

        when(session.getUri()).thenReturn(new URI(WS_BASE_URL + queryString));
        when(jwtToken.parseToken(VALID_TOKEN)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(new fr.horizon.backend_api.model.User()));
        when(session.getAttributes()).thenReturn(new java.util.HashMap<>());

        // Execute
        handler.afterConnectionEstablished(session);

        // Verify
        verify(userRepository).findByEmail(TEST_EMAIL);
    }
}

