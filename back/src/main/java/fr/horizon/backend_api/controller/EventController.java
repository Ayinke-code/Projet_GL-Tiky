package fr.horizon.backend_api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import fr.horizon.backend_api.model.Event;
import fr.horizon.backend_api.repository.EventRepository;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:3000")
public class EventController {

    @Autowired
    private EventRepository repository;

    // Récupérer tous les événements (ou filtrer par mois idéalement)
    @GetMapping
    public List<Event> getAllEvents() {
        return repository.findAll();
    }

    // Ajouter un événement
    @PostMapping
    public Event createEvent(@RequestBody Event event) {
        return repository.save(event);
    }

    // Supprimer un événement
    @DeleteMapping("/{id}")
    public void deleteEvent(@PathVariable Long id) {
        repository.deleteById(id);
    }

    // Modifier un événement
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
