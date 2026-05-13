package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.CategoryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "category")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryStatus status = CategoryStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    private String subject;

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "category_followers", joinColumns = @JoinColumn(name = "category_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> followers = new HashSet<>();

    /**
     * Date et heure de création de la catégorie.
     */
    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Date et heure de la dernière modification.
     */
    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;

    /**
     * Préchargement des données avant la persistance.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Mise à jour de la date de modification.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
