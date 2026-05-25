package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.IssueFollower;
import fr.horizon.backend_api.model.Notification;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.NotificationType;
import fr.horizon.backend_api.repository.IssueFollowerRepository;
import fr.horizon.backend_api.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour NotificationService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private IssueFollowerRepository issueFollowerRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User user;
    private User actor;
    private Issue issue;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        actor = new User();
        actor.setId(2L);

        issue = new Issue();
        issue.setId(1L);
        issue.setTitle("Test issue");
    }

    /** Vérifie qu'une notification est bien créée et sauvegardée */
    @Test
    void createNotification_success() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.createNotification(
                user, issue, NotificationType.NEW_MESSAGE, "Nouveau message");

        assertNotNull(result);
        assertEquals(NotificationType.NEW_MESSAGE, result.getType());
        verify(notificationRepository, times(1)).save(any());
    }

    /** Vérifie que notifyFollowers crée une notification pour chaque follower sauf l'acteur */
    @Test
    void notifyFollowers_excludesActor() {
        IssueFollower follower = new IssueFollower();
        follower.setUser(user);

        when(issueFollowerRepository.findByUserNotAndIssue(actor, issue))
                .thenReturn(List.of(follower));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.notifyFollowers(issue, NotificationType.NEW_MESSAGE, "Nouveau message", actor);

        verify(notificationRepository, times(1)).save(any());
    }

    /** Vérifie que markAsRead lève 404 si la notification n'existe pas */
    @Test
    void markAsRead_notFound_throws404() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                notificationService.markAsRead(99L, user));
    }

    /** Vérifie que markAsRead lève 403 si la notification n'appartient pas à l'utilisateur */
    @Test
    void markAsRead_wrongUser_throws403() {
        Notification notification = new Notification();
        notification.setUser(actor);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(ResponseStatusException.class, () ->
                notificationService.markAsRead(1L, user));
    }

    /** Vérifie que markAllAsRead marque toutes les notifications non lues */
    @Test
    void markAllAsRead_success() {
        Notification n1 = new Notification();
        Notification n2 = new Notification();

        when(notificationRepository.findByUserAndIsReadFalse(user))
                .thenReturn(List.of(n1, n2));

        notificationService.markAllAsRead(user);

        assertTrue(n1.isRead());
        assertTrue(n2.isRead());
        verify(notificationRepository, times(1)).saveAll(any());
    }
}