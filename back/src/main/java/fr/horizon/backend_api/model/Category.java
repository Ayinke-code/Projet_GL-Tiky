package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.CategoryStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entité représentant une catégorie de problèmes.
 *
 * @author Tiky Team
 * @version 1.0
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "category", uniqueConstraints = {
        @UniqueConstraint(name = "uk_category_name", columnNames = { "name" })
})
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private CategoryStatus status = CategoryStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;
}
