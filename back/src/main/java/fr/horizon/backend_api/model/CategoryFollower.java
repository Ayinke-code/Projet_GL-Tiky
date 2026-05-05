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

    @Column(nullable = false)
    private String teacherId;

    @Column(nullable = false)
    private Long categoryId;
}
