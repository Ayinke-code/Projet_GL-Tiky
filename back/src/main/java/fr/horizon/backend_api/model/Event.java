package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.CategoryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "events")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = true, columnDefinition = "varchar(50)")
    private CategoryType categoryType; // AUDIT, DEFENSE, MEETING, ISSUE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column
    private LocalDateTime startDate;

    @Column
    private LocalDateTime endDate;

    /** Groupes projet affectés à cet événement (AUDIT ou DEFENSE) */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_project_groups",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    @Builder.Default
    private Set<StudentGroup> projectGroups = new HashSet<>();

}
