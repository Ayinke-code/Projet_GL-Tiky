package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.RelationStatus;
import fr.horizon.backend_api.model.enums.RelationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "issue_relation")
public class IssueRelation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Issue sourceIssue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private Issue targetIssue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationType type;

    /** Indique si le lien a été créé par un manager */
    @Column(name = "is_manager", nullable = false)
    private boolean manager;

    /** Utilisateur ayant créé le lien */
    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /**
     * Statut du lien.
     * PENDING  : en attente de validation du second manager
     * OFFICIAL : validé par les deux managers
     * REJECTED : refusé par le second manager
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RelationStatus status;

    /**
     * Manager qui doit valider le lien (manager de l'issue cible).
     * Null si les deux issues ont le même manager — le lien est directement OFFICIAL.
     */
    @ManyToOne
    @JoinColumn(name = "validator_id")
    private User validator;
}