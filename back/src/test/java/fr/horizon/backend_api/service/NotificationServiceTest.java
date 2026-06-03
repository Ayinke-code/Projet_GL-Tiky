package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Event;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.Notification;
import fr.horizon.backend_api.model.RequestManager;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.NotificationDto;
import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import fr.horizon.backend_api.model.enums.TypeNotification;
import fr.horizon.backend_api.repository.NotificationRepository;
import fr.horizon.backend_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Issue testIssue;
    private RequestManager testRequestManager;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@horizon.fr");

        testIssue = new Issue();
        testIssue.setId(10L);

        User targetTeacher = new User();
        targetTeacher.setName("M. Prof");

        testRequestManager = new RequestManager();
        testRequestManager.setRequestId(50L);
        testRequestManager.setTargetTeacher(targetTeacher);
        testRequestManager.setIssue(testIssue);

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
        // Précondition
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(100L);
            return n;
        });

        // Action
        notificationService.validatationTicketNotification(testUser, testIssue);

        // Assertion
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
    @DisplayName("Ne doit rien envoyer si le statut de RequestManager est différent de ACCEPTED ou DENIED")
    void managerDemandNotification_ShouldNotSend_WhenOtherStatus() {
        // Ex: PENDING
        testRequestManager.setStatus(RequestManagerStatus.PENDING);

        notificationService.managerDemandNotification(testUser, testRequestManager);

        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
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
    @DisplayName("Lier les identifiants des entités liées (Issue, Event, RequestManager)")
    void toDto_ShouldMapFieldsCorrectly_WithRelations() {
        // Précondition
        Event event = new Event();
        event.setId(20L);

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
        assertEquals(50L, dto.getRequestManagerId()); // Vérifie la nouvelle relation
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
}