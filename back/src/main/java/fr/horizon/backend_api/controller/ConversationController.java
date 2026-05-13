package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.service.ConversationService;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.HashMap;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final UserRepository userRepository;

    @GetMapping("/issue/{issueId}")
    public ResponseEntity<?> getConversationByIssue(@PathVariable Long issueId) {
        Optional<Conversation> conversation = conversationService.getConversationByIssueId(issueId);

        if (conversation.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("conversation", null);
            return ResponseEntity.ok(response);
        }

        Conversation conv = conversation.get();

        // Get messages
        List<Message> messages = conversationService.getMessagesForConversation(conv.getId());
        List<Map<String, Object>> messagesList = new java.util.ArrayList<>();
        for (Message msg : messages) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", msg.getUser().getId());
            userMap.put("name", msg.getUser().getName());
            userMap.put("email", msg.getUser().getEmail());
            userMap.put("role", msg.getUser().getDisplayRole());
            userMap.put("roles", msg.getUser().getRoleNames());

            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("id", msg.getId());
            msgMap.put("content", msg.getContent());
            msgMap.put("user", userMap);
            msgMap.put("createdAt", msg.getCreatedAt());
            messagesList.add(msgMap);
        }

        // Get users in conversation
        List<ConversationUser> conversationUsers = conversationService.getUsersInConversation(conv.getId());
        List<Map<String, Object>> usersList = new java.util.ArrayList<>();
        for (ConversationUser cu : conversationUsers) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", cu.getUser().getId());
            userMap.put("name", cu.getUser().getName());
            userMap.put("email", cu.getUser().getEmail());
            userMap.put("role", cu.getUser().getDisplayRole()); // Utilisation du rôle métier
            userMap.put("roles", cu.getUser().getRoleNames());
            userMap.put("conversationRole", cu.getRole().toString());
            usersList.add(userMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", conv.getId());
        response.put("messages", messagesList);
        response.put("users", usersList);
        response.put("createdAt", conv.getCreatedAt());

        Map<String, Object> finalResponse = new HashMap<>();
        finalResponse.put("conversation", response);
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<Map<String, Object>> addMessage(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> payload,
            Authentication auth) {

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String content = payload.get("content");
        if (content != null && content.length() > 4000) {
            throw new RuntimeException("Le message dépasse la limite de 4000 caractères");
        }
        Long issueId = Long.parseLong(payload.get("issueId"));

        Message message = conversationService.addMessageToConversation(conversationId, content, user, issueId);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", message.getUser().getId());
        userMap.put("name", message.getUser().getName());
        userMap.put("email", message.getUser().getEmail());
        userMap.put("role", message.getUser().getDisplayRole());
        userMap.put("roles", message.getUser().getRoleNames());

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("id", message.getId());
        messageMap.put("content", message.getContent());
        messageMap.put("user", userMap);
        messageMap.put("createdAt", message.getCreatedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(messageMap);
    }
}





