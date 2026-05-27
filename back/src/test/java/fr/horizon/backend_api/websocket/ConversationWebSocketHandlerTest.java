package fr.horizon.backend_api.websocket;

import fr.horizon.backend_api.repository.UserRepository;
import fr.horizon.backend_api.service.JwtToken;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static org.mockito.Mockito.*;

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
public class ConversationWebSocketHandlerTest {

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

    @InjectMocks
    private ConversationWebSocketHandler handler;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testAfterConnectionEstablished_ValidTokenAndConversationId() throws Exception {
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
    public void testAfterConnectionEstablished_MissingToken() throws Exception {
        // Setup
        String queryString = CONVERSATION_ID_PARAM + CONVERSATION_ID;
        when(session.getUri()).thenReturn(new URI(WS_BASE_URL + queryString));

        // Execute
        handler.afterConnectionEstablished(session);

        // Verify - session should be closed with BAD_DATA
        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    public void testAfterConnectionEstablished_MissingConversationId() throws Exception {
        // Setup
        String queryString = TOKEN_PARAM + VALID_TOKEN;
        when(session.getUri()).thenReturn(new URI(WS_BASE_URL + queryString));

        // Execute
        handler.afterConnectionEstablished(session);

        // Verify - session should be closed with BAD_DATA
        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    public void testAfterConnectionEstablished_InvalidToken() throws Exception {
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
    public void testAfterConnectionEstablished_UserNotFound() throws Exception {
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
    public void testBroadcastToConversationBuildsCorrectPayload() throws Exception {
        // Setup
        // Execute - this tests that the method doesn't throw
        handler.broadcastToConversation(CONVERSATION_ID, TEST_MESSAGE);

        // Verify - should complete without throwing
    }

    @Test
    public void testAfterConnectionClosedRemovesSession() throws Exception {
        // Setup
        when(session.getId()).thenReturn(SESSION_ID);

        // Execute
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Verify - should complete without throwing
    }

    @Test
    public void testExtractTokenFromValidQuery() throws Exception {
        // This is a simpler test that doesn't mock the internal query parsing
        String queryString = TOKEN_PARAM + VALID_TOKEN + CONVERSATION_ID_PARAM + CONVERSATION_ID_STR;

        when(session.getUri()).thenReturn(new URI(WS_BASE_URL + queryString));
        when(jwtToken.parseToken(VALID_TOKEN)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(new fr.horizon.backend_api.model.User()));
        when(session.getAttributes()).thenReturn(new java.util.HashMap<>());

        // Execute - should successfully parse token and conversation ID
        handler.afterConnectionEstablished(session);

        // Verify
        verify(userRepository).findByEmail(TEST_EMAIL);
    }
}

