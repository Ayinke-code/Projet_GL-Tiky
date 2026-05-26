package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.dto.EventRequestDto;
import fr.horizon.backend_api.model.dto.EventResponseDto;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Event;
import fr.horizon.backend_api.model.StudentGroup;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.model.enums.RoleEnum;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.EventRepository;
import fr.horizon.backend_api.repository.StudentGroupRepository;
import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.exception.EventNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final StudentGroupRepository studentGroupRepository;
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

        if (category.getCategoryType() == CategoryType.AUDIT || category.getCategoryType() == CategoryType.DEFENSE) {
            boolean hasTeacherRole = user.getRoles().stream()
                    .anyMatch(r -> RoleEnum.TEACHER.value().equals(r.getName()) || RoleEnum.ADMIN.value().equals(r.getName()));
            if (!hasTeacherRole) {
                throw new AccessDeniedException("Seuls les enseignants peuvent créer des événements de type " + category.getCategoryType());
            }
        }

        if (category.getCategoryType() == CategoryType.AUDIT || category.getCategoryType() == CategoryType.DEFENSE) {
            if (request.getStartDate() == null || request.getEndDate() == null) {
                throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires pour un événement de ce type.");
            }
            if (!request.getEndDate().isAfter(request.getStartDate())) {
                throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");
            }
        }

        Event event = Event.builder()
                .name(request.getName())
                .description(request.getDescription())
                .categoryType(category.getCategoryType())
                .category(category)
                .createdBy(user)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        // Affectation des groupes projet (AUDIT / DEFENSE uniquement)
        if ((category.getCategoryType() == CategoryType.AUDIT || category.getCategoryType() == CategoryType.DEFENSE)
                && request.getProjectGroupIds() != null && !request.getProjectGroupIds().isEmpty()) {
            Set<StudentGroup> groups = new HashSet<>(studentGroupRepository.findAllById(request.getProjectGroupIds()));
            event.setProjectGroups(groups);
        }

        Event saved = eventRepository.save(event);
        return mapToResponse(saved);
    }

    /**
     * Récupère la liste des événements d’un type donné, triés par date décroissante.
     *
     * @param type type d’événement ({@code AUDIT} ou {@code DEFENSE})
     * @return liste de DTO correspondant aux événements trouvés
     */
    public List<EventResponseDto> listByType(CategoryType type) {
        return eventRepository.findByCategoryTypeWithGroups(type)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void deleteById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        eventRepository.delete(event);
    }

    /**
     * Convertit une entité {@link Event} en DTO de réponse.
     *
     * @param event entité événement à convertir
     * @return DTO contenant les informations publiques de l’événement
     */
    private EventResponseDto mapToResponse(Event event) {
        List<EventResponseDto.ProjectGroupRefDto> groups = event.getProjectGroups().stream()
                .map(g -> {
                    // Enseignants : ceux du groupe parent (OPTION) s'il existe, sinon ceux du groupe lui-même
                    Set<String> teacherNames = (g.getParentGroup() != null
                            ? g.getParentGroup().getTeachers()
                            : g.getTeachers())
                            .stream()
                            .map(fr.horizon.backend_api.model.User::getName)
                            .collect(java.util.stream.Collectors.toSet());
                    return EventResponseDto.ProjectGroupRefDto.builder()
                            .id(g.getId())
                            .name(g.getName())
                            .categoryName(g.getCategory().getName())
                            .parentGroupName(g.getParentGroup() != null ? g.getParentGroup().getName() : null)
                            .teachers(teacherNames)
                            .build();
                })
                .toList();

        return EventResponseDto.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .categoryType(event.getCategoryType())
                .categoryName(event.getCategory().getName())
                .createdBy(event.getCreatedBy().getName())
                .createdAt(event.getCreatedAt())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .projectGroups(groups)
                .build();
    }
}
