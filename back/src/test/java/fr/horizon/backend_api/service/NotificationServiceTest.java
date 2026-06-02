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
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@horizon.fr");

        testNotification = Notification.builder()
                .id(100L)
                .content("Message de test")
                .isRead(false)
                .type(TypeNotification.NEW_ISSUE)
                .recipient(testUser)
                .build();
    }


    @Test
    @DisplayName("Sauvegarder en BDD et envoyer la rétroaction WebSocket lors du test de connexion")
    void loginNotificationTest_ShouldSaveAndSendNotification_WhenUserExists() {
        // Précondition
        String email = "test@horizon.fr";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // Action
        notificationService.loginNotificationTest(email);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertEquals("Tu es connecté", notificationCaptor.getValue().getContent());
        assertFalse(notificationCaptor.getValue().isRead());
        assertEquals(testUser, notificationCaptor.getValue().getRecipient());

        //rétroaction
        ArgumentCaptor<NotificationDto> dtoCaptor = ArgumentCaptor.forClass(NotificationDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/user/1"), dtoCaptor.capture());
        assertEquals(100L, dtoCaptor.getValue().getId());
    }

    @Test
    @DisplayName("Doit lever une exception si l'utilisateur n'existe pas lors de la création d'une notification")
    void loginNotificationTest_ShouldThrowException_WhenUserNotFound() {
        // précondition
        String email = "inconnu@horizon.fr";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Action et assertion
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            notificationService.loginNotificationTest(email);
        });

        assertEquals("User not found", exception.getMessage());

        // retroaction
        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }


    @Test
    @DisplayName("Marquer la notification comme lue et la sauvegarder")
    void markAsRead_ShouldUpdateAndSave_WhenNotificationExists() {
        // précondition
        Long notifId = 100L;
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(testNotification));

        // Action
        notificationService.markAsRead(notifId);

        // Assertion
        assertTrue(testNotification.isRead());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    @DisplayName("Rien faire si la notification à marquer comme lue n'existe pas")
    void markAsRead_ShouldDoNothing_WhenNotificationDoesNotExist() {
        // précondition
        Long notifId = 999L;
        when(notificationRepository.findById(notifId)).thenReturn(Optional.empty());

        // action
        notificationService.markAsRead(notifId);

        // assertion
        verify(notificationRepository, never()).save(any());
    }


    @Test
    @DisplayName("Doit retourner la liste des notifications non lues sous la forme du DTo")
    void getUserNewNotification_ShouldReturnMappedDtoList() {
        // précondition
        Long userId = 1L;
        when(notificationRepository.findByIsReadAndRecipientIdOrderByIdDesc(false, userId))
                .thenReturn(List.of(testNotification));

        // action
        List<NotificationDto> results = notificationService.getUserNewNotification(userId);

        // Assertion
        assertNotNull(results);
        assertEquals(1, results.size());

        NotificationDto dto = results.get(0);
        assertEquals(100L, dto.getId());
        assertEquals("Message de test", dto.getContent());
        assertFalse(dto.isRead());
        assertEquals(TypeNotification.NEW_ISSUE, dto.getType());
    }


    @Test
    @DisplayName("Retourner null si Notification est null")
    void toDto_ShouldReturnNull_WhenNotificationIsNull() {
        assertNull(notificationService.toDto(null));
    }

    @Test
    @DisplayName("Lier les identifiants des entités liées")
    void toDto_ShouldMapFieldsCorrectly_WithIssueAndEvent() {
        // Précondition
        Issue issue = new Issue();
        issue.setId(10L);

        Event event = new Event();
        event.setId(20L);

        Notification complexNotif = Notification.builder()
                .id(200L)
                .type(TypeNotification.UPDATING_EVENT)
                .isRead(true)
                .content("Event updated")
                .issue(issue)
                .event(event)
                .build();

        // Action
        NotificationDto dto = notificationService.toDto(complexNotif);

        // Assertion
        assertNotNull(dto);
        assertEquals(200L, dto.getId());
        assertEquals(TypeNotification.UPDATING_EVENT, dto.getType());
        assertTrue(dto.isRead());
        assertEquals("Event updated", dto.getContent());
        assertEquals(10L, dto.getIssueId());
        assertEquals(20L, dto.getEventId());
    }

    @Test
    @DisplayName("La notification n'est liée ni à une Issue ni à un Event")
    void toDto_ShouldHandleNullRelationsSafely() {
        // Action
        NotificationDto dto = notificationService.toDto(testNotification);

        // Assertion
        assertNotNull(dto);
        assertNull(dto.getIssueId());
        assertNull(dto.getEventId());
    }
}