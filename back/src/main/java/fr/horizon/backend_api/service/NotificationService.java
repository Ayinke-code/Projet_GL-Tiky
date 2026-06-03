package fr.horizon.backend_api.service;


import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.Notification;
import fr.horizon.backend_api.model.RequestManager;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.NotificationDto;
import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import fr.horizon.backend_api.model.enums.TypeNotification;
import fr.horizon.backend_api.repository.NotificationRepository;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String USER_NOT_FOUND = "User not found";

    private static final String VALIDATE_TICKET_MESSAGE = "Votre ticket vient d'être validé";
    private static final String NEW_TICKET_CATEGORIE_MESSAGE = "Un nouveau ticket a été dans votre catégorie, affectez-le rapidement à un professeur !";
    private static final String INCOMPLET_TICKET_MESSAGE = "Votre ticket a été marqué incomplet, complétez-le rapidement !";
    private static final String DEMANDE_MANAGER_MESSAGE = "Vous avez reçu une nouvelle demande de manager";


    /**
     * Sauvegarde la notification en base et l'envoie via WebSocket à l'utilisateur cible.
     * * @param notification La notification à sauvegarder.
     * @param notification L'ID de l'utilisateur qui doit recevoir la notification.
     */
    private void saveAndSend(Notification notification) {
        Notification savedNotification = notificationRepository.save(notification);

        NotificationDto dto = toDto(savedNotification);
        // Abonnement du front à ce topic précis pour recevoir les messages.
        String destination = "/topic/notifications/user/" + notification.getRecipient().getId();

        messagingTemplate.convertAndSend(destination, dto);

    }


    /**
     * Notifier le créateur d'une issue que son issue a été validé
     */
    @Transactional
    public void validatationTicketNotification(User user, Issue issue){

        Notification notif = Notification.builder()
                .content(VALIDATE_TICKET_MESSAGE)
                .isRead(false)
                .issue(issue)
                .recipient(user)
                .type(TypeNotification.VALIDE_ISSUE)
                .build();

        saveAndSend(notif);
    }

    /**
     * Notifier un professeur qu'il a recu une demande de manager
     */
    @Transactional
    public void reveivedManagerRequest(User user, RequestManager requestManager){

        Notification notif = Notification.builder()
                .content(DEMANDE_MANAGER_MESSAGE)
                .isRead(false)
                .requestManager(requestManager)
                .recipient(user)
                .type(TypeNotification.NEW_DEMANDE_MANAGER)
                .build();

        saveAndSend(notif);
    }

    /**
     * Notifier le créateur d'une issue que son issue a été marqué incomplète
     */
    @Transactional
    public void incompleteTicketNotification(User user, Issue issue){

        Notification notif = Notification.builder()
                .content(INCOMPLET_TICKET_MESSAGE)
                .isRead(false)
                .issue(issue)
                .recipient(user)
                .type(TypeNotification.INCOMPLETE_ISSUE)
                .build();

        saveAndSend(notif);
    }

    /**
     * Notifier le categorie Owner de la création d'un nouveau ticket dans sa catégorie
     */
    @Transactional
    public void creationTicketNotification(User user, Issue issue){
        Notification notif = Notification.builder()
                .content(NEW_TICKET_CATEGORIE_MESSAGE)
                .isRead(false)
                .issue(issue)
                .recipient(user)
                .type(TypeNotification.NEW_ISSUE_IN_CATEGORIE)
                .build();

        saveAndSend(notif);
    }

    /**
     * Notifier le categorie Owner de la mise a jour de la demande
     */
    @Transactional
    public void managerDemandNotification(User user, RequestManager requestManager) {

        String teacherName = requestManager.getTargetTeacher().getName();

        String issueId =  String.valueOf(requestManager.getIssue().getId());

        String content;

        if (requestManager.getStatus() == RequestManagerStatus.ACCEPTED) {
            content = "Votre demande a été acceptée par " + teacherName + ". Le ticket " + issueId + " est maintenant ouvert !";
        } else if (requestManager.getStatus() == RequestManagerStatus.DENIED) {
            content = "Votre demande a été refusée par " + teacherName + ". Demandez vite à un autre professeur.";
        } else {
            return;
        }

        Notification notif = Notification.builder()
                .isRead(false)
                .content(content)
                .requestManager(requestManager)
                .recipient(user)
                .type(TypeNotification.RESPONSE_TO_MANAGER_ISSUE)
                .build();

        saveAndSend(notif);
    }


    /**
     * Marquer une notification comme lu
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notif -> {
            notif.setRead(true);
            notificationRepository.save(notif);
        });
    }


    /**
     * Récupère les notifications que l'utilisateur a pas encore lu.
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
                .sendingHour(LocalDateTime.now())
                .content(notification.getContent())
                .issueId(notification.getIssue() != null ? notification.getIssue().getId() : null)
                .eventId(notification.getEvent() != null ? notification.getEvent().getId() : null)
                .requestManagerId(notification.getRequestManager() != null ? notification.getRequestManager().getRequestId(): null)
                .build();
    }
}