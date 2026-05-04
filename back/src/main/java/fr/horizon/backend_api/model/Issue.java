package fr.horizon.backend_api.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ticket")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTick;

    private String titre;

    private String description;

    private String statut = "en attente de traitement";

    private Boolean estPrive = false;

    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

    private String typeRejet;

    private String raisonRejet;

    private String commentaire;

    @ManyToOne
    @JoinColumn(name = "id_cat", nullable = false)
    private Category categorie;

    @ManyToOne
    @JoinColumn(name = "id_util", nullable = false)
    private User createur;

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
