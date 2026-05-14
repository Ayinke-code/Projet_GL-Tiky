package fr.horizon.backend_api.model;

import jakarta.persistence.*;
import lombok.*;
import fr.horizon.backend_api.model.enums.IssueStatus;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "issue")
public class Issue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private IssueStatus status = IssueStatus.OUVERT;

    @Builder.Default
    @Column(nullable = false)
    private boolean isPrivate = false;

    private String rejectType;

    private String rejectReason;

    private String rejectComment;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "id_util_1")
    private User manager;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<Conversation> conversations = new java.util.HashSet<>();

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<Document> documents = new java.util.HashSet<>();

}