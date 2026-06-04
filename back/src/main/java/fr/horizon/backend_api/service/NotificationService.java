package fr.horizon.backend_api.service;


import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.NotificationDto;
import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import fr.horizon.backend_api.model.enums.TypeNotification;
import fr.horizon.backend_api.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;


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
     * Notifier un utilisateur qu'il a été invité à un événement
     */
    @Transactional
    public void inviteToEventNotification(User user, Event event) {
        String content;

        //(ex: "14/06/2026 à 10:30")
        String formattedDate = "à une date ultérieure";
        if (event.getStartDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
            formattedDate = "le " + event.getStartDate().format(formatter);
        }

        switch (event.getCategoryType()) {
            case AUDIT ->
                    content = "Vous avez un audit prévu " + formattedDate + " avec votre groupe. Consultez vite votre calendrier !";

            case DEFENSE ->
                    content = "Vous avez une soutenance prévue " + formattedDate + ". Consultez vite votre calendrier !";

            case ISSUE -> {
                String issueName = (event.getIssue() != null) ? "« " + event.getIssue().getTitle() + " »" : "";
                content = "Vous avez un rendez-vous à propos du ticket " + issueName + ". Confirmez vite votre présence !";
            }

            default ->
                    content = "Vous avez été invité à un événement " + formattedDate + ". Consultez-le vite !";
        }

        Notification notif = Notification.builder()
                .content(content)
                .isRead(false)
                .recipient(user)
                .issue(event.getIssue())
                .event(event)
                .type(TypeNotification.NEW_EVENT_INVITATION)
                .build();

        saveAndSend(notif);
    }

    /**
     * Notifier les participants d'un événement d'un changement d'horaire
     */
    @Transactional
    public void updateEventNotification(User user, Event event) {

        String issueName = (event.getIssue() != null) ? " « " + event.getIssue().getTitle() + " »" : "";

        String content = "L'heure du rendez-vous pour le ticket" + issueName + " a été modifiée. Vérifiez les nouveaux horaires rapidement !";

        Notification notif = Notification.builder()
                .content(content)
                .isRead(false)
                .recipient(user)
                .event(event)
                .type(TypeNotification.UPDATE_EVENT)
                .build();

        saveAndSend(notif);
    }

    /**
     * Notifier le créateur d'un événement de la réponse d'un invité (acceptation ou refus).
     */
    @Transactional
    public void responseEventNotification(User user, Event event, String guest, boolean response) {
        String content;

        if (response) {
            content = guest + " a accepté votre demande de rendez-vous. Consultez le statut des autres participants.";
        } else {
            content = guest + " a décliné l'invitation au rendez-vous en raison d'une indisponibilité.";
        }

        Notification notif = Notification.builder()
                .content(content)
                .isRead(false)
                .recipient(user)
                .event(event)
                .type(TypeNotification.EVENT_RESPONSE)
                .build();

        saveAndSend(notif);
    }

    public void cancelEventNotification(User user, Event event){
        String content = event.getName()+ "a été annulé, consultez votre calendrier !";
        Notification notif = Notification.builder()
                .content(content)
                .isRead(false)
                .recipient(user)
                .type(TypeNotification.EVENT_CANCEL)
                .build();

        saveAndSend(notif);
    }

    public void reopeningDemandNotification(User user, Issue issue){
        String content = issue.getCreatedBy().getName() + " demande une réouverture du ticket: "+ issue.getTitle()+". Répondez-lui vite !";

        Notification notif = Notification.builder()
                .isRead(false)
                .content(content)
                .issue(issue)
                .recipient(user)
                .type(TypeNotification.NEW_OPEN_ISSUE)
                .build();

        saveAndSend(notif);
    }

    public void reopeningDemandAcceptedNotification(User user, Issue issue){
        String content = issue.getManager().getName() + " a accepté votre demande, votre ticket: "+ issue.getTitle()+ " est de nouveau disponible. Consultez le rapidement !";

        Notification notif = Notification.builder()
                .isRead(false)
                .content(content)
                .issue(issue)
                .recipient(user)
                .type(TypeNotification.ACCEPTED_OPEN_ISSUE)
                .build();

        saveAndSend(notif);
    }

    public void closeIssueNotification(User user, Issue issue){
        String content ="votre ticket: "+ issue.getTitle()+ " vient d'être cloturé, consultez le vite";

        Notification notif = Notification.builder()
                .isRead(false)
                .content(content)
                .issue(issue)
                .recipient(user)
                .type(TypeNotification.CLOSING_ISSUE)
                .build();

        saveAndSend(notif);
    }

    public void openIssueNotification(User user, Issue issue){
        String content ="votre ticket: "+ issue.getTitle()+ " vient d'être rouvert, consultez le vite";

        Notification notif = Notification.builder()
                .isRead(false)
                .content(content)
                .issue(issue)
                .recipient(user)
                .type(TypeNotification.OPEN_ISSUE)
                .build();

        saveAndSend(notif);
    }

    /**
     * Notifier le manager d'un nouveau follower
     */
    @Transactional
    public void newFollowerNotification(User user, Issue issue, User follower){
        String content = follower.getName() +" s'est abonné à votre ticket: "+issue.getTitle();

        Notification notif = Notification.builder()
                .content(content)
                .isRead(false)
                .issue(issue)
                .recipient(user)
                .type(TypeNotification.NEW_FOLLOWER)
                .build();

        saveAndSend(notif);
    }

    /**
     * Notifier le manager lors d'unfollow
     */
    @Transactional
    public void newUnfollowerNotification(User user, Issue issue, User follower){
        String content = follower.getName() +" s'est désabonné de votre ticket: "+issue.getTitle();

        Notification notif = Notification.builder()
                .content(content)
                .isRead(false)
                .issue(issue)
                .recipient(user)
                .type(TypeNotification.NEW_UNFOLLOW)
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
                .eventCategory(notification.getEvent() != null && notification.getEvent().getCategoryType() != null
                        ? notification.getEvent().getCategoryType().name() : null)
                .requestManagerId(notification.getRequestManager() != null ? notification.getRequestManager().getRequestId(): null)
                .build();
    }
}