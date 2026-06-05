package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "document")
public class Document extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_doc")
    private Long id;

    // nom original affiché à l'utilisateur
    @Column(nullable = false)
    private String originalName;

    // nom physique sur disque (UUID.pdf)
    @Column(nullable = false, unique = true)
    private String storedName;

    // Type de document
    @Enumerated(EnumType.STRING)
    private DocumentType type;

    // Exemple de chemin relatif vers un fichier uploadé
    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @ManyToOne
    @JoinColumn(name = "id_tick", nullable = false)
    private Issue issue;

    @ManyToOne
    @JoinColumn(name = "id_user", nullable = false)
    private User uploadedBy;
}

