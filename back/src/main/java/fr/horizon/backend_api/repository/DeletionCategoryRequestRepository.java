package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.DeletionCategoryRequest;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.CategoryRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeletionCategoryRequestRepository extends JpaRepository<DeletionCategoryRequest, Long> {
    boolean existsByCategoryAndRequesterAndStatus(Category category, User requester, CategoryRequestStatus status);

    List<DeletionCategoryRequest> findByCategory_Owner_IdAndStatus(Long ownerId, CategoryRequestStatus status);
}
