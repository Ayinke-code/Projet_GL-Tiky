package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.CategoryRequestStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "category_request")
public class CategoryRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String subject;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryRequestStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requester_id")
    private User requester;

    @ManyToOne
    @JoinColumn(name = "validator_id")
    private User validator;

    private String rejectionReason;

    private String rejectionComment;

    @OneToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
