package fr.horizon.backend_api.model;

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

    /** Indique si le lien a été créé par un manager (visible par tous) ou un étudiant (visible par lui et les managers) */
    @Column(name = "is_manager", nullable = false)
    private boolean manager;

    /** Utilisateur ayant créé le lien */
    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}