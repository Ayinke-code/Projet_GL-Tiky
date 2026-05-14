package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.CategoryFollower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryFollowerRepository extends JpaRepository<CategoryFollower, Long> {
    List<CategoryFollower> findByUser(fr.horizon.backend_api.model.User user);
    Optional<CategoryFollower> findByUserAndCategory(fr.horizon.backend_api.model.User user, fr.horizon.backend_api.model.Category category);
    void deleteByUserAndCategory(fr.horizon.backend_api.model.User user, fr.horizon.backend_api.model.Category category);
    boolean existsByUserAndCategory(fr.horizon.backend_api.model.User user, fr.horizon.backend_api.model.Category category);
}