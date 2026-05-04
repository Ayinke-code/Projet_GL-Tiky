package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByEstValideTrue();

    List<Category> findByCategoryOwner_Id(Long ownerId);
}
