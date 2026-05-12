package fr.horizon.backend_api.model;

import java.time.LocalDateTime;

import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "request_manager")

public class RequestManager extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @Enumerated(EnumType.STRING)
    private RequestManagerStatus status = RequestManagerStatus.PENDING;


    private LocalDateTime processingDate;

    @ManyToOne
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne
    @JoinColumn(name = "user1_id", nullable = true)
    private User targetTeacher;

    @ManyToOne
    @JoinColumn(name = "user2_id", nullable = false)
    private User categoryOwner;

}
