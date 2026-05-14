package fr.horizon.backend_api.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final IssueWebSocketHandler issueWebSocketHandler;

    public WebSocketConfig(IssueWebSocketHandler issueWebSocketHandler) {
        this.issueWebSocketHandler = issueWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(issueWebSocketHandler, "/ws/issue")
                .setAllowedOrigins("*");
    }
}