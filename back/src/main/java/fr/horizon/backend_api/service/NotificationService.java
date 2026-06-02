package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Event;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.Notification;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.NotificationDto;
import fr.horizon.backend_api.model.enums.TypeNotification;
import fr.horizon.backend_api.repository.NotificationRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String USER_NOT_FOUND = "User not found";


    /**
     * Sauvegarde la notification en base et l'envoie via WebSocket à l'utilisateur cible.
     * * @param notification La notification à sauvegarder.
     * @param email L'ID de l'utilisateur qui doit recevoir la notification.
     */
    private void saveAndSend(Notification notification, String email) {
        Notification savedNotification = notificationRepository.save(notification);

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        NotificationDto dto = toDto(savedNotification);
        // Abonnement du front à ce topic précis pour recevoir les messages.
        String destination = "/topic/notifications/user/" + currentUser.getId();
        messagingTemplate.convertAndSend(destination, dto);

        notification.setRecipient(currentUser);

    }

    @Transactional
    public void loginNotificationTest( String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        Notification notif = Notification.builder()
                .content("Tu es connecté")
                .isRead(false)
                .recipient(currentUser)
                .build();

        saveAndSend(notif, email);
    }


    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notif -> {
            notif.setRead(true);
            notificationRepository.save(notif);
        });
    }


    /**
     * Récupère l'historique complet des notifications pour un utilisateur donné.
     */
    public List<NotificationDto> getUserNewNotification(Long userId) {
        return notificationRepository.findByIsReadAndRecipientIdOrderByIdDesc(false,userId)
                .stream()
                .map(this::toDto)
                .toList();

    }

    /**
     * Convertit une entité Notification en NotificationDto
     */
    public NotificationDto toDto(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationDto.builder()
                .id(notification.getId())
                .type(notification.getType())
                .isRead(notification.isRead())
                .content(notification.getContent())
                // On vérifie si l'issue existe avant de récupérer son ID pour éviter un NullPointerException
                .issueId(notification.getIssue() != null ? notification.getIssue().getId() : null)
                // Idem pour l'événement
                .eventId(notification.getEvent() != null ? notification.getEvent().getId() : null)
                .build();
    }
}