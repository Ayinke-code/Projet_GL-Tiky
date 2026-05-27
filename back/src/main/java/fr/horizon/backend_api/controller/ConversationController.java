package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.service.ConversationService;
import fr.horizon.backend_api.websocket.ConversationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
/**
 * Contrôleur REST des conversations.
 * <p>
 * Délègue toute la logique métier au service afin de rester centré sur
 * le transport HTTP et la diffusion WebSocket.
 */
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationWebSocketHandler conversationWebSocketHandler;

    /**
     * Récupère la conversation associée à une issue.
     *
     * @param issueId identifiant de l'issue
     * @return réponse HTTP contenant la conversation sérialisée
     */
    @GetMapping("/issue/{issueId}")
    public ResponseEntity<Map<String, Object>> getConversationByIssue(@PathVariable Long issueId) {
        return ResponseEntity.ok(conversationService.getConversationByIssuePayload(issueId));
    }

    /**
     * Crée ou récupère une conversation pour une issue (idempotent).
     * <p>
     * Si la conversation n'existe pas, elle est créée.
     * Si elle existe déjà, elle est retournée.
     *
     * @param issueId identifiant de l'issue
     * @return réponse HTTP contenant la conversation sérialisée
     */
    @PostMapping("/issue/{issueId}/initialize")
    public ResponseEntity<Map<String, Object>> initializeConversationForIssue(@PathVariable Long issueId) {
        return ResponseEntity.ok(conversationService.getOrCreateConversationForIssue(issueId));
    }

    /**
     * Ajoute un message à une conversation.
     *
     * @param conversationId identifiant de la conversation
     * @param payload contenu de la requête (message + issue)
     * @param auth utilisateur authentifié
     * @return message créé au format JSON
     */
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<Map<String, Object>> addMessage(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> payload,
            Authentication auth) {

        ConversationService.MessageDispatchResult result = conversationService.addMessage(conversationId, payload, auth.getName());
        // Broadcast message via WebSocket to all connected clients on this conversation
        conversationWebSocketHandler.broadcastToConversation(conversationId, result.broadcastPayload());
        return ResponseEntity.status(HttpStatus.CREATED).body(result.message());
    }
}

