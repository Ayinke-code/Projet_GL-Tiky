package fr.horizon.backend_api.model;

import jakarta.persistence.*;
import lombok.*;
import fr.horizon.backend_api.model.enums.ConversationRole;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "convers_util")
public class ConversationUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_convs_util")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationRole role;

    @ManyToOne
    @JoinColumn(name = "id_convs", nullable = false)
    private Conversation conversation;

    @ManyToOne
    @JoinColumn(name = "id_util", nullable = false)
    private User user;
}