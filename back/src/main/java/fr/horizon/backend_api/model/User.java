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
@Table(name = "Utilisateur")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_util")
    private Long id;

    @Column(name = "nom")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "mdp")
    private String password;

    @ManyToOne
    @JoinColumn(name = "id_util_1")
    private Role role;

    @Column(name = "date_creation")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}