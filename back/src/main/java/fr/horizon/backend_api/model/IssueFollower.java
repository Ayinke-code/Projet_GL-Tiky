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
@Table(name = "abonne_tick")
public class IssueFollower {

    /** Identifiant unique de l'abonnement */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAbnTick;

    /**
     * Raison du suivi de l'issue.
     * INTERESTED = consultation uniquement
     * ABLE_TO_HELP = peut interagir (répondre, commenter)
     * IMPACTED = peut interagir (répondre, commenter)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "raison", nullable = false)
    private FollowedReason raison;

    /** Date de création de l'abonnement, générée automatiquement */
    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    /** Issue suivie */
    @ManyToOne
    @JoinColumn(name = "id_tick", nullable = false)
    private Issue issue;

    /** Utilisateur qui suit l'issue */
    @ManyToOne
    @JoinColumn(name = "id_util", nullable = false)
    private User createur;

    /** Initialise la date de création avant la persistance */
    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
    }
}