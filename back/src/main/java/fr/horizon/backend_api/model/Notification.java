package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entité représentant une notification envoyée à un utilisateur.
 * Liée à une issue et indique le type d'action déclencheur.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notification")
public class Notification extends BaseEntity {

    /** Identifiant unique de la notification */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type d'action ayant déclenché la notification.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    /** Contenu lisible de la notification */
    @Column(name = "content", nullable = false)
    private String content;

    /** Indique si la notification a été lue */
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    /** Issue concernée par la notification */
    @ManyToOne
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    /** Utilisateur destinataire de la notification */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}