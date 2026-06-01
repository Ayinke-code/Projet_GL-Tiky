package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.CategoryType;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@Table(name = "events")

public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private Long id_evt;
    private String title;
    @JsonProperty("start") // Sera envoyé comme "start" au front
    private LocalDateTime start_date;
    @JsonProperty("end") // Sera envoyé comme "end" au front
    private LocalDateTime end_date;
    private String description;
    private String statut;
    private String color; // Pour le style du calendrier

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = true, columnDefinition = "varchar(50)")
    private CategoryType categoryType; // AUDIT, DEFENSE, MEETING, ISSUE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = true)
    private Issue issue;

    @Column
    private LocalDateTime startDate;

    @Column
    private LocalDateTime endDate;

    @Column
    private Boolean validated = true;

    /** Groupes projet affectés à cet événement (AUDIT ou DEFENSE) */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_project_groups",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    @Builder.Default
    private Set<StudentGroup> projectGroups = new HashSet<>();

    @Builder.Default
    @OneToMany(
            mappedBy = "event",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<EventParticipant> participants = new HashSet<>();

    public void addParticipant(User user, Boolean hasAccepted) {
        EventParticipant participation = EventParticipant.builder()
                .event(this)
                .user(user)
                .hasAccepted(hasAccepted)
                .build();
        this.participants.add(participation);
    }

    public void deleteParticipant(User user) {
        if (user == null || this.participants == null) {
            return;
        }

        this.participants.removeIf(participant -> {
            boolean match = participant.getUser().getId().equals(user.getId());
            if (match) {
                participant.setEvent(null);
            }
            return match;
        });
    }

}
