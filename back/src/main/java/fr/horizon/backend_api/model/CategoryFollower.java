package fr.horizon.backend_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category_followers")
@IdClass(CategoryFollowerId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryFollower {

    @Id
    @Column(name = "user_id", nullable = false)
    private String teacherId;

    @Id
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
}
