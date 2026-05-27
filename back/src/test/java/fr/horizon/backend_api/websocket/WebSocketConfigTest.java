package fr.horizon.backend_api.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;

import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour la configuration WebSocket.
 * <p>
 * Teste que les handlers WebSocket sont correctement enregistrés
 * sur les bons endpoints et avec les bonnes origines.
 */
public class WebSocketConfigTest {

    private static final String CONVERSATION_ENDPOINT = "/ws/conversation";
    private static final String ALLOWED_ORIGIN = "*";

    @Mock
    private ConversationWebSocketHandler conversationWebSocketHandler;

    @Mock
    private WebSocketHandlerRegistry registry;

    @Mock
    private WebSocketHandlerRegistration registration;

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testRegisterWebSocketHandlers_ConversationHandlerRegistered() {
        // Setup
        when(registry.addHandler(conversationWebSocketHandler, CONVERSATION_ENDPOINT))
                .thenReturn(registration);
        when(registration.setAllowedOrigins(ALLOWED_ORIGIN))
                .thenReturn(registration);

        // Execute
        webSocketConfig.registerWebSocketHandlers(registry);

        // Verify - conversation handler is registered on the correct endpoint
        verify(registry).addHandler(conversationWebSocketHandler, CONVERSATION_ENDPOINT);
        verify(registration).setAllowedOrigins(ALLOWED_ORIGIN);
    }

    @Test
    public void testRegisterWebSocketHandlers_OnlyConversationHandlerRegistered() {
        // Setup
        when(registry.addHandler(conversationWebSocketHandler, CONVERSATION_ENDPOINT))
                .thenReturn(registration);
        when(registration.setAllowedOrigins(ALLOWED_ORIGIN))
                .thenReturn(registration);

        // Execute
        webSocketConfig.registerWebSocketHandlers(registry);

        // Verify - only conversation handler should be registered (issue handler removed)
        verify(registry, times(1)).addHandler(any(), anyString());
        verify(registry).addHandler(conversationWebSocketHandler, CONVERSATION_ENDPOINT);
    }
}

