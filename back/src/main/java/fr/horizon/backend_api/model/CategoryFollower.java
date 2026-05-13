package fr.horizon.backend_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category_followers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryFollower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String teacherId;

    private Long categoryId;

    @Column(nullable = false)
    @Builder.Default
    private String status = "ACCEPTED";
}
