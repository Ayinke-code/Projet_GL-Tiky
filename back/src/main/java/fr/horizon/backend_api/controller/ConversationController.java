package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.service.ConversationService;
import fr.horizon.backend_api.websocket.IssueWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final IssueWebSocketHandler issueWebSocketHandler;

    @GetMapping("/issue/{issueId}")
    public ResponseEntity<?> getConversationByIssue(@PathVariable Long issueId) {
        return ResponseEntity.ok(conversationService.getConversationByIssuePayload(issueId));
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<Map<String, Object>> addMessage(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> payload,
            Authentication auth) {

        ConversationService.MessageDispatchResult result = conversationService.addMessage(conversationId, payload, auth.getName());
        issueWebSocketHandler.broadcastToIssue(result.issueId(), result.broadcastPayload());
        return ResponseEntity.status(HttpStatus.CREATED).body(result.message());
    }
}

