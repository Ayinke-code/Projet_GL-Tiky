package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.GroupType;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "student_groups")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** Groupe d'option parent (uniquement renseigné pour les groupes PROJECT) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_group_id")
    private StudentGroup parentGroup;

    /** Promotion associée (pour les groupes OPTION) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private StudentGroup promotion;

    /** Année de la promotion (uniquement pour les groupes PROMOTION) */
    @Column(name = "promotion_year")
    private Integer year;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "student_group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @Builder.Default
    private Set<User> members = new HashSet<>();
}
