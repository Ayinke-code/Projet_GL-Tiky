package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.MergeRequestStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "merge_request")
public class MergeRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_issue_id", nullable = false)
    private Issue sourceIssue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_issue_id", nullable = false)
    private Issue targetIssue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MergeRequestStatus status = MergeRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by_id")
    private User targetManager;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "source_manager_approved")
    private Boolean sourceManagerApproved;

    @Column(name = "target_manager_approved")
    private Boolean targetManagerApproved;
}