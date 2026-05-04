package fr.horizon.backend_api.model;

import java.time.LocalDateTime;

import fr.horizon.backend_api.repository.IssueRepository;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Demande_cat")

public class DemandeManager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDemManager;

    @Enumerated(EnumType.STRING)
    private DemandeManagerStatus statut = DemandeManagerStatus.PENDING;

    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

    private LocalDateTime dateTraitement;

    @ManyToOne
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne
    @JoinColumn(name = "id_util", nullable = false)
    private User targetTeacher;

    @ManyToOne
    @JoinColumn(name = "id_util_1")
    private User categorieOwner;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
        this.dateModification = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.dateModification = LocalDateTime.now();
    }
}
