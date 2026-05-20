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
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de gestion des événements académiques.
 * <p>
 * Fournit les opérations métier liées aux événements (audits et soutenances) :
 * création avec contrôle de rôle, listing par type et conversion en DTO.
 * <p>
 * La création d’un événement AUDIT ou DEFENSE est réservée aux utilisateurs
 * ayant le rôle {@code TEACHER} ou {@code ADMIN}.
 */
@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    /**
     * Crée un nouvel événement académique.
     * <p>
     * Vérifie que l’utilisateur possède le rôle requis avant la persistance.
     *
     * @param request données de création de l’événement
     * @param email   email de l’utilisateur créateur, extrait du contexte de sécurité
     * @return événement créé sous forme de DTO
     * @throws AccessDeniedException si l’utilisateur n’a pas le rôle TEACHER ou ADMIN
     * @throws CategoryNotFoundException si la catégorie n’existe pas
     */
    public EventResponseDto create(EventRequestDto request, String email) {
        User user = userService.findByEmail(email);
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));

        if (category.getEventType() == EventType.AUDIT || category.getEventType() == EventType.DEFENSE) {
            boolean hasTeacherRole = user.getRoles().stream()
                    .anyMatch(r -> "TEACHER".equals(r.getName()) || "ADMIN".equals(r.getName()));
            if (!hasTeacherRole) {
                throw new AccessDeniedException("Seuls les enseignants peuvent créer des événements de type " + category.getEventType());
            }
        }

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

    /**
     * Récupère la liste des événements d’un type donné, triés par date décroissante.
     *
     * @param type type d’événement ({@code AUDIT} ou {@code DEFENSE})
     * @return liste de DTO correspondant aux événements trouvés
     */
    public List<EventResponseDto> listByType(EventType type) {
        return eventRepository.findByEventTypeOrderByCreatedAtDesc(type)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une entité {@link Event} en DTO de réponse.
     *
     * @param event entité événement à convertir
     * @return DTO contenant les informations publiques de l’événement
     */
    private EventResponseDto mapToResponse(Event event) {
        return EventResponseDto.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .eventType(event.getEventType())
                .categoryName(event.getCategory().getName())
                .createdBy(event.getCreatedBy().getName())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
