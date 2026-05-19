package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.dto.EventRequestDto;
import fr.horizon.backend_api.model.dto.EventResponseDto;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Event;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.EventType;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.EventRepository;
import fr.horizon.backend_api.exception.CategoryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    public EventResponseDto create(EventRequestDto request, String email) {
        User user = userService.findByEmail(email);
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));

        Event event = Event.builder()
                .name(request.getName())
                .description(request.getDescription())
                .eventType(category.getEventType())
                .category(category)
                .createdBy(user)
                .build();

        Event saved = eventRepository.save(event);
        return mapToResponse(saved);
    }

    public List<EventResponseDto> listByType(EventType type) {
        return eventRepository.findByEventTypeOrderByCreatedAtDesc(type)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private EventResponseDto mapToResponse(Event event) {
        return EventResponseDto.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .eventType(event.getEventType())
                .categoryName(event.getCategory().getName())
                .createdBy(event.getCreatedBy().getEmail())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
