package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    List<Availability> findByUserId(Long userId);

    Optional<Availability> findByIdAndUserId(Long id, Long userId);
}
