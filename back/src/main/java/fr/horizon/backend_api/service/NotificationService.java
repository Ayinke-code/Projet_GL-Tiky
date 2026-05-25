package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.IssueFollower;
import fr.horizon.backend_api.model.Notification;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.NotificationType;
import fr.horizon.backend_api.repository.IssueFollowerRepository;
import fr.horizon.backend_api.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Service gérant la création et la gestion des notifications.
 * Les notifications sont déclenchées à chaque action sur une issue suivie.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final IssueFollowerRepository issueFollowerRepository;

    /**
     * Crée et enregistre une notification pour un utilisateur.
     *
     * @param user    destinataire de la notification
     * @param issue   issue concernée
     * @param type    type d'action déclencheur
     * @param content contenu lisible de la notification
     * @return la notification créée
     */
    public Notification createNotification(User user, Issue issue, NotificationType type, String content) {
        Notification notification = Notification.builder()
                .user(user)
                .issue(issue)
                .type(type)
                .content(content)
                .build();
        return notificationRepository.save(notification);
    }

    /**
     * Notifie tous les followers d'une issue d'une action effectuée.
     * Exclut l'utilisateur à l'origine de l'action.
     *
     * @param issue   issue concernée
     * @param type    type d'action déclencheur
     * @param content contenu lisible de la notification
     * @param actor   utilisateur à l'origine de l'action (exclu des destinataires)
     */
    public void notifyFollowers(Issue issue, NotificationType type, String content, User actor) {
        List<IssueFollower> followers;
        if (actor == null) {
            followers = issueFollowerRepository.findByIssue(issue);
        } else {
            followers = issueFollowerRepository.findByUserNotAndIssue(actor, issue);
        }
        followers.forEach(follower -> createNotification(follower.getUser(), issue, type, content));
    }

    /**
     * Récupère toutes les notifications d'un utilisateur, triées par date décroissante.
     *
     * @param user utilisateur concerné
     * @return liste des notifications
     */
    public List<Notification> getNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Marque une notification comme lue.
     *
     * @param id   identifiant de la notification
     * @param user utilisateur concerné
     * @throws ResponseStatusException 404 si la notification n'existe pas
     * @throws ResponseStatusException 403 si la notification n'appartient pas à l'utilisateur
     */
    public void markAsRead(Long id, User user) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Notification non trouvée"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Accès non autorisé");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Marque toutes les notifications d'un utilisateur comme lues.
     *
     * @param user utilisateur concerné
     */
    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserAndIsReadFalse(user);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}