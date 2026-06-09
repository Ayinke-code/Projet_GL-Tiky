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
@Table(name = "deletion_category_request")
public class DeletionCategoryRequest extends BaseEntity{
sdfdsfsdfsdfsd
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requester_id")
    private User requester;

    @ManyToOne
    @JoinColumn(name = "validator_id")
    private User validator;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryRequestStatus status = CategoryRequestStatus.PENDING;

}
