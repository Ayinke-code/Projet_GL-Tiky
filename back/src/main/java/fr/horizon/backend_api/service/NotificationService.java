package fr.horizon.backend_api.service;


import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.NotificationDto;
import fr.horizon.backend_api.model.enums.LinkRejectionReason;
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
    private static final String MESSAGE_CONSULTE = ". Consultez-le vite !";


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
                    content = "Vous avez été invité à un événement " + formattedDate + MESSAGE_CONSULTE;
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
        String content ="Votre ticket: "+ issue.getTitle()+ " vient d'être cloturé, consultez le vite";

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
        String content ="Votre ticket: "+ issue.getTitle()+ " vient d'être rouvert, consultez le vite";

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
     * Notifier le manager lorsque d'une demande de lien est fait (cas étudiant)
     */
    @Transactional
    public void newLinkRequestNotification(User user, OfficialLinkRequest officialLinkRequest) {
        Issue userIssue = officialLinkRequest.getTargetIssue();
        Issue otherIssue = officialLinkRequest.getSourceIssue();

        boolean isUserManagerOfSource = officialLinkRequest.getSourceIssue().getManager() != null
                && officialLinkRequest.getSourceIssue().getManager().getId().equals(user.getId());

        if (isUserManagerOfSource) {
            userIssue = officialLinkRequest.getSourceIssue();
            otherIssue = officialLinkRequest.getTargetIssue();
        }

        String content = "Plusieurs utilisateurs ont lié votre ticket: " + userIssue.getTitle() +
                " avec le ticket: " + otherIssue.getTitle() + MESSAGE_CONSULTE;

        Notification notif = Notification.builder()
                .content(content)
                .isRead(false)
                .officialLinkRequest(officialLinkRequest)
                .issue(userIssue)
                .recipient(user)
                .type(TypeNotification.SUBSCRIBERS_NEW_LINK)
                .build();

        saveAndSend(notif);
    }

    @Transactional
    public void newLinkRequestManagerNotification(User user, OfficialLinkRequest officialLinkRequest, User target){
        String content = user.getName() + " demande une liaison entre votre ticket: "+ officialLinkRequest.getTargetIssue().getTitle()+ " et " + officialLinkRequest.getSourceIssue().getTitle();

        Notification notif = Notification.builder()
                .content(content)
                .isRead(false)
                .officialLinkRequest(officialLinkRequest)
                .issue(officialLinkRequest.getTargetIssue())
                .recipient(target)
                .type(TypeNotification.MANAGER_LINK_REQUEST)
                .build();

        saveAndSend(notif);
    }


    @Transactional
    public void followersNewLinkNotification(User user, Issue issue, IssueRelation issueRelation) {

        Issue otherIssue = issue.getId().equals(issueRelation.getSourceIssue().getId())
                ? issueRelation.getTargetIssue()
                : issueRelation.getSourceIssue();

        String content = "Le ticket que vous suivez: " + issue.getTitle() +
                " est maintenant lié au ticket: " + otherIssue.getTitle() +
                MESSAGE_CONSULTE;

        Notification notif = Notification.builder()
                .content(content)
                .isRead(false)
                .issueRelation(issueRelation)
                .issue(issue)
                .recipient(user)
                .type(TypeNotification.FOLLOWERS_LINK)
                .build();

        saveAndSend(notif);
    }

    @Transactional
    public void linkIssueResponseNotification(User user, OfficialLinkRequest request) {
        Issue source = request.getSourceIssue();
        Issue target = request.getTargetIssue();

        User responder = (source.getManager() != null && source.getManager().getId().equals(user.getId()))
                ? target.getManager()
                : source.getManager();

        String responderName = (responder != null) ? responder.getName() : "Un manager";
        String content;

        if (Boolean.TRUE.equals(request.getApproved())) {
            content = responderName + " a accepté votre demande de liaison ! Les tickets sont maintenant liés.";
        } else {
            content = responderName + " a refusé votre demande de liaison entre les tickets.";

            LinkRejectionReason reason = request.getRejectionReason();
            if (reason != null) {
                String reasonLabel = switch (reason) {
                    case DUPLICATE_RELATION -> "Lien en doublon ou redondant";
                    case IRRELEVANT -> "Tickets sans lien réel (non pertinent)";
                    case WRONG_RELATION_TYPE -> "Type de relation incorrect";
                    case OTHER -> "Autre raison";
                };
                content += " Motif : " + reasonLabel + ".";
            }
        }

        Notification notif = Notification.builder()
                .content(content)
                .isRead(false)
                .officialLinkRequest(request)
                .issue(source)
                .recipient(user)
                .type(TypeNotification.MANAGER_RESPONSE_LINK_REQUEST)
                .build();

        saveAndSend(notif);
    }

    public void mergeRequestNotification(MergeRequest mergeRequest) {
        User requester = mergeRequest.getRequester();

        User targetToNotify = (mergeRequest.getSourceIssue().getManager() != null && mergeRequest.getSourceIssue().getManager().getId().equals(requester.getId()))
                ? mergeRequest.getTargetIssue().getManager()
                : mergeRequest.getSourceIssue().getManager();

        if (targetToNotify != null) {
            String content = requester.getName() + " souhaite fusionner son ticket: " + mergeRequest.getSourceIssue().getTitle() + " avec votre ticket: " + mergeRequest.getTargetIssue().getTitle() + ". Répondez-lui vite !";

            Notification notif = Notification.builder()
                    .content(content)
                    .isRead(false)
                    .mergeRequest(mergeRequest)
                    .issue(mergeRequest.getTargetIssue())
                    .recipient(targetToNotify)
                    .type(TypeNotification.MERGE_REQUEST_MANAGER)
                    .build();

            saveAndSend(notif);
        }
    }

    public void mergeRequestDeniedNotification(MergeRequest mergeRequest){
        User requester = mergeRequest.getRequester();
        User target = mergeRequest.getTargetManager();
        String content = target.getName() + " a refusé votre demande de fusion entre votre ticket: "+mergeRequest.getTargetIssue().getTitle()+" et "+mergeRequest.getSourceIssue().getTitle()+". Pour motif: "+mergeRequest.getRejectionReason()+".";

        Notification notif = Notification.builder()
                .content(content)
                .isRead(false)
                .mergeRequest(mergeRequest)
                .issue(mergeRequest.getSourceIssue())
                .recipient(requester)
                .type(TypeNotification.MERGE_REQUEST_DENIED)
                .build();

        saveAndSend(notif);
    }

    public void mergeRequestAcceptedNotification(MergeRequest mergeRequest){
        User requester = mergeRequest.getRequester();
        User target = mergeRequest.getTargetManager();
        String content = target.getName() + " a accepté votre demande de fusion entre votre ticket: "+mergeRequest.getSourceIssue().getTitle()+" et "+mergeRequest.getTargetIssue().getTitle()+". Consultez le nouveau ticket !";

        Notification notif = Notification.builder()
                .content(content)
                .isRead(false)
                .mergeRequest(mergeRequest)
                .issue(mergeRequest.getTargetIssue())
                .recipient(requester)
                .type(TypeNotification.MERGE_REQUEST_ACCEPTED)
                .build();

        saveAndSend(notif);
    }

    public void mergeRequestNotifieFollower(User user,Issue issue){
        String content = "Le ticket: "+issue.getTitle()+" que vous suivez, a été fusionné, consultez le nouveau ticket rapidement !";

        Notification notif = Notification.builder()
                .content(content)
                .isRead(false)
                .issue(issue)
                .recipient(user)
                .type(TypeNotification.MERGE_REQUEST_FOLLOWERS)
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
                .officialLinkRequestId(notification.getOfficialLinkRequest() != null ? notification.getOfficialLinkRequest().getId() : null)
                .issueId(notification.getIssue() != null ? notification.getIssue().getId() : null)
                .eventId(notification.getEvent() != null ? notification.getEvent().getId() : null)
                .eventCategory(notification.getEvent() != null && notification.getEvent().getCategoryType() != null
                        ? notification.getEvent().getCategoryType().name() : null)
                .requestManagerId(notification.getRequestManager() != null ? notification.getRequestManager().getRequestId(): null)
                .build();
    }
}