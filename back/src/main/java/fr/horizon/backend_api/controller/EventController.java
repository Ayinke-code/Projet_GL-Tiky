package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.EventRequestDto;
import fr.horizon.backend_api.model.dto.EventResponseDto;
import fr.horizon.backend_api.model.enums.EventType;
import fr.horizon.backend_api.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponseDto> create(@RequestBody EventRequestDto request, Authentication authentication) {
        String email = authentication.getName();
        EventResponseDto response = eventService.create(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<EventResponseDto>> listByType(@PathVariable EventType type) {
        List<EventResponseDto> events = eventService.listByType(type);
        return ResponseEntity.ok(events);
    }
}

