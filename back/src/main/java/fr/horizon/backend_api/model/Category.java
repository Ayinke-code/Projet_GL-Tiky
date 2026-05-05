package fr.horizon.backend_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entité représentant une catégorie de questions.
 * 
 * @author Tiky Team
 * @version 1.0
 */
@Entity
@Table(name = "categories", uniqueConstraints = {
        @UniqueConstraint(name = "uk_category_name", columnNames = { "name", })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    /**
     * Identifiant unique de la catégorie.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom de la catégorie (unique, insensible à la casse).
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Statut de la catégorie (active, inactive, archived).
     */
    @Column(nullable = false)
    @Builder.Default
    private String status = "active";

    /**
     * Identifiant de l'enseignant propriétaire de la catégorie.
     */
    @Column(nullable = false)
    private String categoryOwner;

    private String subject;

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
