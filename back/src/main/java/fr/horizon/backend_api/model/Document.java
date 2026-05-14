package fr.horizon.backend_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "document")
@AttributeOverride(name = "createdAt", column = @Column(name = "date_creation", nullable = false, updatable = false))
public class Document extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_doc")
    private Long id;

    @Column(nullable = false)
    private String url;

    @ManyToOne
    @JoinColumn(name = "id_tick", nullable = false)
    private Issue issue;
}