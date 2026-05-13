package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.CategoryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "category")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String subject;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryStatus status = CategoryStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "category_followers", joinColumns = @JoinColumn(name = "category_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> followers = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
