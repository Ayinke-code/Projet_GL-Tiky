package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.Notification;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.service.IssueFollowerService;
import fr.horizon.backend_api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST des notifications.
 * Expose les endpoints permettant de récupérer et gérer les notifications.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final IssueFollowerService issueFollowerService;

    /**
     * Récupère toutes les notifications de l'utilisateur courant.
     *
     * @param auth contexte d'authentification Spring Security
     * @return liste des notifications
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(Authentication auth) {
        User user = issueFollowerService.getUserByEmail(auth.getName());
        return ResponseEntity.ok(notificationService.getNotifications(user));
    }

    /**
     * Marque une notification comme lue.
     *
     * @param id   identifiant de la notification
     * @param auth contexte d'authentification Spring Security
     * @return réponse vide avec statut 204
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication auth) {
        User user = issueFollowerService.getUserByEmail(auth.getName());
        notificationService.markAsRead(id, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Marque toutes les notifications de l'utilisateur comme lues.
     *
     * @param auth contexte d'authentification Spring Security
     * @return réponse vide avec statut 204
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication auth) {
        User user = issueFollowerService.getUserByEmail(auth.getName());
        notificationService.markAllAsRead(user);
        return ResponseEntity.noContent().build();
    }
}