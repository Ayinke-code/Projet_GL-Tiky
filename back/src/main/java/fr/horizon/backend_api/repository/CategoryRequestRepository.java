package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.CategoryRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRequestRepository extends JpaRepository<CategoryRequest, Long> {
}
