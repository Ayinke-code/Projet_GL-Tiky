package fr.horizon.backend_api.controller;


import fr.horizon.backend_api.model.dto.NotificationDto;
import fr.horizon.backend_api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Marque une notification spécifique comme lue.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);

        return ResponseEntity.ok().build();
    }

    /**
     * Récupère les notifcations pas encore lu par l'utilisateur pendant qu'il était hors ligne
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationDto>> getUserNewNotification(@PathVariable Long userId) {
        List<NotificationDto> history = notificationService.getUserNewNotification(userId);
        return ResponseEntity.ok(history);
    }
}
