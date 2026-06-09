package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.NotificationDto;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.model.enums.LinkRejectionReason;
import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import fr.horizon.backend_api.model.enums.TypeNotification;
import fr.horizon.backend_api.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Issue testIssue;
    private RequestManager testRequestManager;
    private Notification testNotification;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@horizon.fr");
        testUser.setName("Jean Dupont");

        User targetTeacher = new User();
        targetTeacher.setId(2L);
        targetTeacher.setName("M. Prof");

        testIssue = new Issue();
        testIssue.setId(10L);
        testIssue.setTitle("Bug Connexion");
        testIssue.setCreatedBy(testUser);
        testIssue.setManager(targetTeacher);

        testRequestManager = new RequestManager();
        testRequestManager.setRequestId(50L);
        testRequestManager.setTargetTeacher(targetTeacher);
        testRequestManager.setIssue(testIssue);

        testEvent = new Event();
        testEvent.setId(20L);
        testEvent.setName("Soutenance Finale");
        testEvent.setStartDate(LocalDateTime.of(2026, 6, 14, 10, 30));
        testEvent.setIssue(testIssue);

        testNotification = Notification.builder()
                .id(100L)
                .content("Message de test")
                .isRead(false)
                .type(TypeNotification.NEW_ISSUE_IN_CATEGORIE)
                .recipient(testUser)
                .build();
    }

    @Test
    @DisplayName("Doit créer et envoyer une notification de validation de ticket")
    void validatationTicketNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(100L);
            return n;
        });

        notificationService.validatationTicketNotification(testUser, testIssue);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        Notification savedNotif = notifCaptor.getValue();
        assertEquals("Votre ticket vient d'être validé", savedNotif.getContent());
        assertEquals(TypeNotification.VALIDE_ISSUE, savedNotif.getType());
        assertEquals(testIssue, savedNotif.getIssue());
        assertEquals(testUser, savedNotif.getRecipient());

        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/user/1"), any(NotificationDto.class));
    }

    @Test
    @DisplayName("Doit créer et envoyer une notification de demande de manager")
    void reveivedManagerRequest_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.reveivedManagerRequest(testUser, testRequestManager);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        assertEquals("Vous avez reçu une nouvelle demande de manager", notifCaptor.getValue().getContent());
        assertEquals(TypeNotification.NEW_DEMANDE_MANAGER, notifCaptor.getValue().getType());
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/user/1"), any(NotificationDto.class));
    }

    @Test
    @DisplayName("Doit créer et envoyer une notification pour ticket incomplet")
    void incompleteTicketNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.incompleteTicketNotification(testUser, testIssue);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        assertEquals("Votre ticket a été marqué incomplet, complétez-le rapidement !", notifCaptor.getValue().getContent());
        assertEquals(TypeNotification.INCOMPLETE_ISSUE, notifCaptor.getValue().getType());
    }

    @Test
    @DisplayName("Doit créer et envoyer une notification pour nouveau ticket dans la catégorie")
    void creationTicketNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.creationTicketNotification(testUser, testIssue);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        assertEquals("Un nouveau ticket a été dans votre catégorie, affectez-le rapidement à un professeur !", notifCaptor.getValue().getContent());
        assertEquals(TypeNotification.NEW_ISSUE_IN_CATEGORIE, notifCaptor.getValue().getType());
    }

    @Test
    @DisplayName("Doit envoyer une notification ACCEPTED pour managerDemandNotification")
    void managerDemandNotification_ShouldSend_WhenAccepted() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        testRequestManager.setStatus(RequestManagerStatus.ACCEPTED);

        notificationService.managerDemandNotification(testUser, testRequestManager);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        assertTrue(notifCaptor.getValue().getContent().contains("Votre demande a été acceptée par M. Prof"));
        assertEquals(TypeNotification.RESPONSE_TO_MANAGER_ISSUE, notifCaptor.getValue().getType());
    }

    @Test
    @DisplayName("Doit envoyer une notification DENIED pour managerDemandNotification")
    void managerDemandNotification_ShouldSend_WhenDenied() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        testRequestManager.setStatus(RequestManagerStatus.DENIED);

        notificationService.managerDemandNotification(testUser, testRequestManager);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        assertTrue(notifCaptor.getValue().getContent().contains("Votre demande a été refusée par M. Prof"));
    }

    @Test
    @DisplayName("Doit créer et envoyer une notification pour un nouvel abonnement (Follow)")
    void newFollowerNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        User follower = new User();
        follower.setName("Alice");

        notificationService.newFollowerNotification(testUser, testIssue, follower);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        Notification savedNotif = notifCaptor.getValue();
        assertTrue(savedNotif.getContent().contains("Alice s'est abonné à votre ticket: Bug Connexion"));
        assertEquals(TypeNotification.NEW_FOLLOWER, savedNotif.getType());
        assertEquals(testIssue, savedNotif.getIssue());
        assertEquals(testUser, savedNotif.getRecipient());
    }

    @Test
    @DisplayName("Doit créer et envoyer une notification pour un désabonnement (Unfollow)")
    void newUnfollowerNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        User unfollower = new User();
        unfollower.setName("Bob");

        notificationService.newUnfollowerNotification(testUser, testIssue, unfollower);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        Notification savedNotif = notifCaptor.getValue();
        assertTrue(savedNotif.getContent().contains("Bob s'est désabonné de votre ticket: Bug Connexion"));
        assertEquals(TypeNotification.NEW_UNFOLLOW, savedNotif.getType());
        assertEquals(testUser, savedNotif.getRecipient());

        assertEquals(testIssue, savedNotif.getIssue());
    }

    @Test
    @DisplayName("Ne doit rien envoyer si le statut de RequestManager est différent de ACCEPTED ou DENIED")
    void managerDemandNotification_ShouldNotSend_WhenOtherStatus() {
        testRequestManager.setStatus(RequestManagerStatus.PENDING);

        notificationService.managerDemandNotification(testUser, testRequestManager);

        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }


    @Test
    @DisplayName("Doit notifier une invitation à un événement de type AUDIT avec date formatée")
    void inviteToEventNotification_ShouldSend_ForAudit() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        testEvent.setCategoryType(CategoryType.AUDIT);

        notificationService.inviteToEventNotification(testUser, testEvent);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        assertEquals(TypeNotification.NEW_EVENT_INVITATION, notifCaptor.getValue().getType());
        assertTrue(notifCaptor.getValue().getContent().contains("Vous avez un audit prévu le 14/06/2026 à 10:30"));
    }

    @Test
    @DisplayName("Doit notifier une invitation à un événement de type ISSUE avec nom du ticket")
    void inviteToEventNotification_ShouldSend_ForIssue() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        testEvent.setCategoryType(CategoryType.ISSUE);

        notificationService.inviteToEventNotification(testUser, testEvent);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        assertTrue(notifCaptor.getValue().getContent().contains("rendez-vous à propos du ticket « Bug Connexion »"));
    }

    @Test
    @DisplayName("Doit notifier la mise à jour d'un événement")
    void updateEventNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.updateEventNotification(testUser, testEvent);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        assertEquals(TypeNotification.UPDATE_EVENT, notifCaptor.getValue().getType());
        assertTrue(notifCaptor.getValue().getContent().contains("L'heure du rendez-vous pour le ticket « Bug Connexion » a été modifiée"));
    }

    @Test
    @DisplayName("Doit notifier l'acceptation d'une invitation à un événement")
    void responseEventNotification_ShouldSend_WhenAccepted() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.responseEventNotification(testUser, testEvent, "Jean Dupont", true);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        assertEquals(TypeNotification.EVENT_RESPONSE, notifCaptor.getValue().getType());
        assertTrue(notifCaptor.getValue().getContent().contains("Jean Dupont a accepté votre demande de rendez-vous"));
    }

    @Test
    @DisplayName("Doit notifier le refus d'une invitation à un événement")
    void responseEventNotification_ShouldSend_WhenDeclined() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.responseEventNotification(testUser, testEvent, "Jean Dupont", false);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        assertTrue(notifCaptor.getValue().getContent().contains("Jean Dupont a décliné l'invitation"));
    }

    @Test
    @DisplayName("Doit notifier l'annulation d'un événement")
    void cancelEventNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.cancelEventNotification(testUser, testEvent);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        assertEquals(TypeNotification.EVENT_CANCEL, notifCaptor.getValue().getType());
        assertTrue(notifCaptor.getValue().getContent().contains("Soutenance Finalea été annulé"));
    }


    @Test
    @DisplayName("Doit créer et envoyer une notification de demande de réouverture")
    void reopeningDemandNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.reopeningDemandNotification(testUser, testIssue);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        Notification savedNotif = notifCaptor.getValue();
        assertTrue(savedNotif.getContent().contains("Jean Dupont demande une réouverture du ticket: Bug Connexion"));
        assertEquals(TypeNotification.NEW_OPEN_ISSUE, savedNotif.getType());
        assertEquals(testIssue, savedNotif.getIssue());
        assertEquals(testUser, savedNotif.getRecipient());
    }

    @Test
    @DisplayName("Doit créer et envoyer une notification d'acceptation de réouverture")
    void reopeningDemandAcceptedNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.reopeningDemandAcceptedNotification(testUser, testIssue);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        Notification savedNotif = notifCaptor.getValue();
        assertTrue(savedNotif.getContent().contains("M. Prof a accepté votre demande, votre ticket: Bug Connexion"));
        assertEquals(TypeNotification.ACCEPTED_OPEN_ISSUE, savedNotif.getType());
        assertEquals(testIssue, savedNotif.getIssue());
        assertEquals(testUser, savedNotif.getRecipient());
    }

    @Test
    @DisplayName("Doit créer et envoyer une notification de clôture de ticket")
    void closeIssueNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.closeIssueNotification(testUser, testIssue);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        Notification savedNotif = notifCaptor.getValue();
        assertTrue(savedNotif.getContent().contains("votre ticket: Bug Connexion vient d'être cloturé"));
        assertEquals(TypeNotification.CLOSING_ISSUE, savedNotif.getType());
        assertEquals(testIssue, savedNotif.getIssue());
        assertEquals(testUser, savedNotif.getRecipient());
    }

    @Test
    @DisplayName("Doit créer et envoyer une notification de réouverture de ticket (OPEN_ISSUE)")
    void openIssueNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.openIssueNotification(testUser, testIssue);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        Notification savedNotif = notifCaptor.getValue();

        assertTrue(savedNotif.getContent().contains("votre ticket: Bug Connexion vient d'être rouvert"));
        assertEquals(TypeNotification.OPEN_ISSUE, savedNotif.getType());
        assertEquals(testIssue, savedNotif.getIssue());
        assertEquals(testUser, savedNotif.getRecipient());
    }


    @Test
    @DisplayName("Marquer la notification comme lue et la sauvegarder")
    void markAsRead_ShouldUpdateAndSave_WhenNotificationExists() {
        Long notifId = 100L;
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(testNotification));

        notificationService.markAsRead(notifId);

        assertTrue(testNotification.isRead());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    @DisplayName("Rien faire si la notification à marquer comme lue n'existe pas")
    void markAsRead_ShouldDoNothing_WhenNotificationDoesNotExist() {
        Long notifId = 999L;
        when(notificationRepository.findById(notifId)).thenReturn(Optional.empty());

        notificationService.markAsRead(notifId);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Doit retourner la liste des notifications non lues sous la forme de DTO")
    void getUserNewNotification_ShouldReturnMappedDtoList() {
        Long userId = 1L;
        when(notificationRepository.findByIsReadAndRecipientIdOrderByIdDesc(false, userId))
                .thenReturn(List.of(testNotification));

        List<NotificationDto> results = notificationService.getUserNewNotification(userId);

        assertNotNull(results);
        assertEquals(1, results.size());

        NotificationDto dto = results.get(0);
        assertEquals(100L, dto.getId());
        assertEquals("Message de test", dto.getContent());
        assertFalse(dto.isRead());
        assertEquals(TypeNotification.NEW_ISSUE_IN_CATEGORIE, dto.getType());
    }

    @Test
    @DisplayName("Retourner null si Notification est null")
    void toDto_ShouldReturnNull_WhenNotificationIsNull() {
        assertNull(notificationService.toDto(null));
    }

    @Test
    @DisplayName("Lier les identifiants des entités liées (Issue, Event, RequestManager) et la Catégorie")
    void toDto_ShouldMapFieldsCorrectly_WithRelations() {
        // Précondition
        Event event = new Event();
        event.setId(20L);
        event.setCategoryType(CategoryType.ISSUE);

        Notification complexNotif = Notification.builder()
                .id(200L)
                .type(TypeNotification.RESPONSE_TO_MANAGER_ISSUE)
                .isRead(true)
                .content("Event updated")
                .issue(testIssue)
                .event(event)
                .requestManager(testRequestManager)
                .build();

        // Action
        NotificationDto dto = notificationService.toDto(complexNotif);

        // Assertion
        assertNotNull(dto);
        assertEquals(200L, dto.getId());
        assertEquals(10L, dto.getIssueId());
        assertEquals(20L, dto.getEventId());
        assertEquals(50L, dto.getRequestManagerId());
        assertEquals("ISSUE", dto.getEventCategory());
    }

    @Test
    @DisplayName("La notification n'est liée ni à une Issue, ni à un Event, ni à un RequestManager")
    void toDto_ShouldHandleNullRelationsSafely() {
        NotificationDto dto = notificationService.toDto(testNotification);

        assertNotNull(dto);
        assertNull(dto.getIssueId());
        assertNull(dto.getEventId());
        assertNull(dto.getRequestManagerId());
    }

    @Test
    @DisplayName("Doit notifier les abonnés lorsqu'une demande de liaison est créée")
    void newLinkRequestNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(i -> i.getArgument(0));

        OfficialLinkRequest request = new OfficialLinkRequest();
        request.setSourceIssue(testIssue);

        Issue targetIssue = new Issue();
        targetIssue.setId(11L);
        targetIssue.setTitle("Autre Ticket");

        request.setTargetIssue(targetIssue);

        notificationService.newLinkRequestNotification(testUser, request);

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository).save(captor.capture());

        Notification notif = captor.getValue();

        assertEquals(TypeNotification.SUBSCRIBERS_NEW_LINK, notif.getType());
        assertTrue(notif.getContent().contains("Bug Connexion"));
        assertTrue(notif.getContent().contains("Autre Ticket"));
    }

    @Test
    @DisplayName("Doit notifier un manager d'une demande de liaison")
    void newLinkRequestManagerNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(i -> i.getArgument(0));

        User target = new User();
        target.setId(99L);
        target.setName("Manager cible");

        OfficialLinkRequest request = new OfficialLinkRequest();
        request.setSourceIssue(testIssue);

        Issue targetIssue = new Issue();
        targetIssue.setTitle("Ticket B");
        request.setTargetIssue(targetIssue);

        notificationService.newLinkRequestManagerNotification(
                testUser,
                request,
                target
        );

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository).save(captor.capture());

        Notification notif = captor.getValue();

        assertEquals(TypeNotification.MANAGER_LINK_REQUEST, notif.getType());
        assertEquals(target, notif.getRecipient());
        assertTrue(notif.getContent().contains("Jean Dupont"));
    }

    @Test
    @DisplayName("Doit notifier les followers lorsqu'une liaison est créée")
    void followersNewLinkNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(i -> i.getArgument(0));

        Issue otherIssue = new Issue();
        otherIssue.setId(99L);
        otherIssue.setTitle("Ticket lié");

        IssueRelation relation = new IssueRelation();
        relation.setSourceIssue(testIssue);
        relation.setTargetIssue(otherIssue);

        notificationService.followersNewLinkNotification(
                testUser,
                testIssue,
                relation
        );

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository).save(captor.capture());

        Notification notif = captor.getValue();

        assertEquals(TypeNotification.FOLLOWERS_LINK, notif.getType());
        assertTrue(notif.getContent().contains("Ticket lié"));
    }

    @Test
    @DisplayName("Doit notifier l'acceptation d'une demande de liaison")
    void linkIssueResponseNotification_ShouldSendAcceptedResponse() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(i -> i.getArgument(0));

        OfficialLinkRequest request = new OfficialLinkRequest();
        request.setSourceIssue(testIssue);

        Issue targetIssue = new Issue();
        User targetManager = new User();
        targetManager.setId(3L);
        targetManager.setName("Marie Martin");
        targetIssue.setManager(targetManager);

        request.setTargetIssue(targetIssue);
        request.setApproved(true);

        notificationService.linkIssueResponseNotification(
                testUser,
                request
        );

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository).save(captor.capture());

        assertTrue(captor.getValue()
                .getContent()
                .contains("a accepté votre demande de liaison"));

        assertEquals(
                TypeNotification.MANAGER_RESPONSE_LINK_REQUEST,
                captor.getValue().getType()
        );
    }

    @Test
    @DisplayName("Doit notifier un refus avec motif DUPLICATE_RELATION")
    void linkIssueResponseNotification_ShouldSendDeniedResponseWithReason() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(i -> i.getArgument(0));

        OfficialLinkRequest request = new OfficialLinkRequest();
        request.setSourceIssue(testIssue);

        Issue targetIssue = new Issue();
        User targetManager = new User();
        targetManager.setId(3L);
        targetManager.setName("Marie Martin");
        targetIssue.setManager(targetManager);

        request.setTargetIssue(targetIssue);
        request.setApproved(false);
        request.setRejectionReason(LinkRejectionReason.DUPLICATE_RELATION);

        notificationService.linkIssueResponseNotification(
                testUser,
                request
        );

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository).save(captor.capture());

        String content = captor.getValue().getContent();

        assertTrue(content.contains("a refusé votre demande"));
        assertTrue(content.contains("Lien en doublon ou redondant"));
    }

    @Test
    @DisplayName("Doit mapper officialLinkRequestId dans le DTO")
    void toDto_ShouldMapOfficialLinkRequestId() {
        OfficialLinkRequest request = new OfficialLinkRequest();
        request.setId(500L);

        Notification notification = Notification.builder()
                .id(1L)
                .officialLinkRequest(request)
                .build();

        NotificationDto dto = notificationService.toDto(notification);

        assertEquals(500L, dto.getOfficialLinkRequestId());
    }


    @Test
    @DisplayName("Doit notifier le manager cible d'une nouvelle demande de fusion")
    void mergeRequestNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // 🛠️ CORRECTION ICI : Pour que la notification parte vers la cible,
        // le demandeur (testUser) doit être le manager du ticket source !
        testIssue.setManager(testUser);

        // Création du ticket cible et de son manager
        Issue targetIssue = new Issue();
        targetIssue.setTitle("Ticket Cible");
        User targetManager = new User();
        targetManager.setId(2L);
        targetManager.setName("M. Prof");
        targetIssue.setManager(targetManager);

        MergeRequest mergeRequest = new MergeRequest();
        mergeRequest.setRequester(testUser); // Jean Dupont
        mergeRequest.setSourceIssue(testIssue); // Bug Connexion
        mergeRequest.setTargetIssue(targetIssue);

        notificationService.mergeRequestNotification(mergeRequest);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification notif = captor.getValue();
        assertEquals(TypeNotification.MERGE_REQUEST_MANAGER, notif.getType());
        assertEquals(targetManager, notif.getRecipient());
        assertTrue(notif.getContent().contains("Jean Dupont souhaite fusionner son ticket: Bug Connexion"));
        assertTrue(notif.getContent().contains("avec votre ticket: Ticket Cible"));
    }

    @Test
    @DisplayName("Doit notifier le demandeur qu'une fusion a été refusée avec le motif")
    void mergeRequestDeniedNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Issue targetIssue = new Issue();
        targetIssue.setTitle("Ticket Cible");
        User targetManager = new User();
        targetManager.setName("M. Prof");

        MergeRequest mergeRequest = new MergeRequest();
        mergeRequest.setRequester(testUser);
        mergeRequest.setSourceIssue(testIssue);
        mergeRequest.setTargetIssue(targetIssue);
        mergeRequest.setTargetManager(targetManager);
        mergeRequest.setRejectionReason("Doublon existant");

        notificationService.mergeRequestDeniedNotification(mergeRequest);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification notif = captor.getValue();
        assertEquals(TypeNotification.MERGE_REQUEST_DENIED, notif.getType());
        assertEquals(testUser, notif.getRecipient());
        assertTrue(notif.getContent().contains("M. Prof a refusé votre demande de fusion"));
        assertTrue(notif.getContent().contains("Pour motif: Doublon existant."));
    }

    @Test
    @DisplayName("Doit notifier le demandeur qu'une fusion a été acceptée")
    void mergeRequestAcceptedNotification_ShouldSaveAndSend() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Issue targetIssue = new Issue();
        targetIssue.setTitle("Ticket Cible");
        User targetManager = new User();
        targetManager.setName("M. Prof");

        MergeRequest mergeRequest = new MergeRequest();
        mergeRequest.setRequester(testUser);
        mergeRequest.setSourceIssue(testIssue);
        mergeRequest.setTargetIssue(targetIssue);
        mergeRequest.setTargetManager(targetManager);

        notificationService.mergeRequestAcceptedNotification(mergeRequest);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification notif = captor.getValue();
        assertEquals(TypeNotification.MERGE_REQUEST_ACCEPTED, notif.getType());
        assertEquals(testUser, notif.getRecipient());
        assertTrue(notif.getContent().contains("M. Prof a accepté votre demande de fusion"));
        assertTrue(notif.getContent().contains("Consultez le nouveau ticket !"));
    }
}