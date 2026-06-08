package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.CategoryRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRequestRepository extends JpaRepository<CategoryRequest, Long> {
    List<CategoryRequest> findAllByCategory(Category category);
    void deleteAllByCategory(Category category);
}
