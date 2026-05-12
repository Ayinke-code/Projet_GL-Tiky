package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.CategoryFollower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryFollowerRepository extends JpaRepository<CategoryFollower, Long> {
//    List<CategoryFollower> findByTeacherId(String teacherId);
//    Optional<CategoryFollower> findByTeacherIdAndCategoryId(String teacherId, Long categoryId);
//    void deleteByTeacherIdAndCategoryId(String teacherId, Long categoryId);
//    boolean existsByTeacherIdAndCategoryId(String teacherId, Long categoryId);
}
