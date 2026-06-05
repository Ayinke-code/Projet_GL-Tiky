package fr.horizon.backend_api.model;

import jakarta.persistence.*;
import lombok.*;
import fr.horizon.backend_api.model.enums.IssueStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private IssueStatus status = IssueStatus.EN_ATTENTE;

    @Builder.Default
    @Column(nullable = false)
    private boolean isPrivate = false;

    private String rejectType;
    @Column
    private String rejectReason;
    @Column
    private String rejectComment;

    @Column
    private String reopeningDescription;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Conversation> conversations = new HashSet<>();

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Message> messages = new HashSet<>();
    @ManyToOne
    @JoinColumn(name = "id_util_1")
    private User manager;
    
    @Builder.Default
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestManager> requests = new ArrayList<>();

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IssueFollower> followers = new ArrayList<>();



}
