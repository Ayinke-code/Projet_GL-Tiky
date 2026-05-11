package fr.horizon.backend_api.model;

import java.time.LocalDateTime;

import fr.horizon.backend_api.model.enums.FollowedReason;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "abonne_tick")
public class IssueFollower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAbnTick;

    @Enumerated(EnumType.STRING)
    @Column(name = "raison", nullable = false)
    private FollowedReason raison;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @ManyToOne
    @JoinColumn(name = "id_tick", nullable = false)
    private Issue issue;

    @ManyToOne
    @JoinColumn(name = "id_util", nullable = false)
    private User createur;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
    }

}