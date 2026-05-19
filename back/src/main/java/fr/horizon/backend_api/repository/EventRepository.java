package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Event;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByEventTypeOrderByCreatedAtDesc(EventType type);
    List<Event> findByCreatedByOrderByCreatedAtDesc(User user);
}

