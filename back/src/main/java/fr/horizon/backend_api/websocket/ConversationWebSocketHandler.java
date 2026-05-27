package fr.horizon.backend_api.websocket;

import fr.horizon.backend_api.repository.UserRepository;
import fr.horizon.backend_api.service.JwtToken;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class ConversationWebSocketHandler extends TextWebSocketHandler {

    private static final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Autowired
    private JwtToken jwtToken;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractTokenFromSession(session);
        Long conversationId = extractConversationIdFromSession(session);

        if (token == null || conversationId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        try {
            Claims claims = jwtToken.parseToken(token);
            String email = claims.getSubject();

            userRepository.findByEmail(email).orElseThrow(() -> new IOException("User not found"));

            session.getAttributes().put("userEmail", email);
            session.getAttributes().put("conversationId", conversationId);

            sessions.add(session);
            log.info("WebSocket connection established for user {} on conversation {}", email, conversationId);
        } catch (Exception e) {
            log.error("WebSocket authentication failed", e);
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("WebSocket connection closed for session {}", session.getId());
    }

    public void broadcastToConversation(Long conversationId, String message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                Long sessionConversationId = (Long) session.getAttributes().get("conversationId");
                if (conversationId.equals(sessionConversationId)) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        log.error("Error broadcasting message to conversation {}", conversationId, e);
                    }
                }
            }
        }
    }

    private String extractTokenFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        return null;
    }

    private Long extractConversationIdFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("conversationId=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("conversationId=")) {
                    try {
                        return Long.parseLong(param.substring(15));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
}

