package fr.horizon.backend_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "message")
@AttributeOverride(name = "createdAt", column = @Column(name = "date_creation", nullable = false, updatable = false))
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mes")
    private Long id;

    @Column(name = "contenu", nullable = false, columnDefinition = "TEXT", length = 4000)
    private String content;

    @ManyToOne
    @JoinColumn(name = "id_util", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "id_tick")
    private Issue issue;

    @ManyToOne
    @JoinColumn(name = "id_doc")
    private Document document;

    @ManyToOne
    @JoinColumn(name = "id_convs", nullable = false)
    private Conversation conversation;
}