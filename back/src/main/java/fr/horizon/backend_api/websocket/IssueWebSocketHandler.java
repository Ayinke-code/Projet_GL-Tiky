package fr.horizon.backend_api.websocket;

import fr.horizon.backend_api.model.Message;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.repository.UserRepository;
import fr.horizon.backend_api.service.ConversationService;
import fr.horizon.backend_api.service.JwtToken;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class IssueWebSocketHandler extends TextWebSocketHandler {

    private static final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Autowired
    private JwtToken jwtToken;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationService conversationService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractTokenFromSession(session);
        Long issueId = extractIssueIdFromSession(session);

        if (token == null || issueId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        try {
            Claims claims = jwtToken.parseToken(token);
            String email = claims.getSubject();

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            session.getAttributes().put("userEmail", email);
            session.getAttributes().put("issueId", issueId);
            session.getAttributes().put("userId", user.getId());

            sessions.add(session);
            log.info("WebSocket connection established for user {} on issue {}", email, issueId);
        } catch (Exception e) {
            log.error("WebSocket authentication failed", e);
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        String payload = textMessage.getPayload();
        JSONObject json = new JSONObject(payload);

        String type = json.optString("type");

        if ("message".equals(type)) {
            handleNewMessage(session, json);
        }
    }

    private void handleNewMessage(WebSocketSession session, JSONObject json) throws IOException {
        String content = json.optString("content");
        Long conversationId = json.optLong("conversationId");
        Long issueId = (Long) session.getAttributes().get("issueId");
        String email = (String) session.getAttributes().get("userEmail");

        if (content == null || content.isEmpty() || conversationId == 0) {
            return;
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return;

        // Persist message
        Message message = conversationService.addMessageToConversation(conversationId, content, user, issueId);

        // Broadcast to all sessions interested in this issue
        JSONObject response = new JSONObject();
        response.put("type", "new_message");

        JSONObject userMap = new JSONObject();
        userMap.put("id", user.getId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getDisplayRole());
        userMap.put("roles", user.getRoleNames());

        JSONObject msgMap = new JSONObject();
        msgMap.put("id", message.getId());
        msgMap.put("content", message.getContent());
        msgMap.put("user", userMap);
        msgMap.put("createdAt", message.getCreatedAt());

        response.put("message", msgMap);
        response.put("issueId", issueId);

        broadcastToIssue(issueId, response.toString());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("WebSocket connection closed for session {}", session.getId());
    }

    public void broadcastToIssue(Long issueId, String message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                Long sessionIssueId = (Long) session.getAttributes().get("issueId");
                if (issueId.equals(sessionIssueId)) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        log.error("Error broadcasting message", e);
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

    private Long extractIssueIdFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("issueId=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("issueId=")) {
                    try {
                        return Long.parseLong(param.substring(8));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
}