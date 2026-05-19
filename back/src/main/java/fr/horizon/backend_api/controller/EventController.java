package fr.horizon.backend_api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import fr.horizon.backend_api.model.Event;
import fr.horizon.backend_api.repository.EventRepository;

@RestController
@RequestMapping("events")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class EventController {

    @Autowired
    private EventRepository repository;

    // Récupérer tous les événements (ou filtrer par mois idéalement)
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<Event> getAllEvents() {
        return repository.findAll();
    }

    // Ajouter un événement
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public Event createEvent(@RequestBody Event event) {
        return repository.save(event);
    }

    // Supprimer un événement
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public void deleteEvent(@PathVariable Long id) {
        repository.deleteById(id);
    }

    // Modifier un événement
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public Event updateEvent(@PathVariable Long id, @RequestBody Event event) {
        Event existingEvent = repository.findById(id).orElseThrow();
        existingEvent.setTitle(event.getTitle());
        existingEvent.setStart_date(event.getStart_date());
        existingEvent.setEnd_date(event.getEnd_date());
        existingEvent.setDescription(event.getDescription());
        existingEvent.setStatut(event.getStatut());
        existingEvent.setColor(event.getColor());
        return repository.save(existingEvent);
    }
}
