package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.LinkRejectionReason;
import fr.horizon.backend_api.model.enums.RelationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "official_link_request")
public class OfficialLinkRequest extends BaseEntity {

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

    @Column(nullable = false)
    private boolean isHandled = false;

    // Indique si le manager du ticket source a approuvé
    @Column(name = "source_manager_approved")
    private Boolean sourceManagerApproved;

    // Indique si le manager du ticket cible a approuvé
    @Column(name = "target_manager_approved")
    private Boolean targetManagerApproved;

    @Column(name = "is_approved")
    private Boolean approved;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason")
    private LinkRejectionReason rejectionReason; // Rempli uniquement si approved = false
}