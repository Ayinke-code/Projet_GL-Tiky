package fr.horizon.backend_api.model;

import java.time.LocalDateTime;

import fr.horizon.backend_api.model.enums.FollowedReason;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entité représentant l'abonnement d'un utilisateur à une issue.
 * Contient la raison du suivi et la date d'abonnement.
 * Table de liaison entre les utilisateurs et les issues.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "issue_follower")
public class IssueFollower {

    /** Identifiant unique de l'abonnement */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Raison du suivi de l'issue.
     * INTERESTED = consultation uniquement
     * ABLE_TO_HELP = peut interagir (répondre, commenter)
     * IMPACTED = peut interagir (répondre, commenter)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private FollowedReason reason;

    /** Date de création de l'abonnement, générée automatiquement */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Issue suivie */
    @ManyToOne
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    /** Utilisateur qui suit l'issue */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Initialise la date de création avant la persistance */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}