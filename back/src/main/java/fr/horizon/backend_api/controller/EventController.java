package fr.horizon.backend_api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import fr.horizon.backend_api.dto.EventResponseDto;
import fr.horizon.backend_api.model.Event;
import fr.horizon.backend_api.repository.EventRepository;

@RestController
@RequestMapping("events")
public class EventController {

    @Autowired
    private EventRepository repository;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<EventResponseDto> getAllEvents() {
        return repository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public EventResponseDto createEvent(@RequestBody EventResponseDto dto) {

        Event event = new Event();
        updateEntity(event, dto);

        return toDto(repository.save(event));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public void deleteEvent(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public EventResponseDto updateEvent(
            @PathVariable Long id,
            @RequestBody EventResponseDto dto
    ) {

        Event event = repository.findById(id)
                .orElseThrow();

        updateEntity(event, dto);

        return toDto(repository.save(event));
    }

    private EventResponseDto toDto(Event event) {
        return new EventResponseDto(
                event.getId(),
                event.getTitle(),
                event.getStart_date(),
                event.getEnd_date(),
                event.getDescription(),
                event.getStatut(),
                event.getColor()
        );
    }

    private void updateEntity(Event event, EventResponseDto dto) {
        event.setTitle(dto.title());
        event.setStart_date(dto.start());
        event.setEnd_date(dto.end());
        event.setDescription(dto.description());
        event.setStatut(dto.statut());
        event.setColor(dto.color());
    }
}