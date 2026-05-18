package fr.horizon.backend_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.horizon.backend_api.model.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

}
