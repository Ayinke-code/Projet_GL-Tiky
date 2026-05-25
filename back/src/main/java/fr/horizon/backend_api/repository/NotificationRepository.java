package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Notification;
import fr.horizon.backend_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository gérant les opérations de base de données pour les notifications.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Récupère toutes les notifications d'un utilisateur, triées par date décroissante.
     *
     * @param user utilisateur concerné
     * @return liste des notifications
     */
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Récupère les notifications non lues d'un utilisateur.
     *
     * @param user utilisateur concerné
     * @return liste des notifications non lues
     */
    List<Notification> findByUserAndIsReadFalse(User user);
}