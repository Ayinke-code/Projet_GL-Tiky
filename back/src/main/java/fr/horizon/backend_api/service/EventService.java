package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.EventConflictException;
import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.EventInvitationResponseDto;
import fr.horizon.backend_api.model.dto.EventRequestDto;
import fr.horizon.backend_api.model.dto.EventResponseDto;
import fr.horizon.backend_api.model.dto.StatutInvitationParticipantDto;
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
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import fr.horizon.backend_api.exception.EventNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final UserService userService;
    private final UserRepository userRepository;
    private final StudentGroupRepository studentGroupRepository;

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
        return eventRepository.findByCategoryTypeOrderByCreatedAtDesc(type)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Invite un utilisateur spécifique à un événement.
     * <p>
     * Vérifie au préalable si l'utilisateur possède un conflit d'agenda sur le créneau de l'événement.
     * Si aucun conflit n'est détecté, l'utilisateur est ajouté à la liste des participants avec un statut "en attente" (null).
     *
     * @param eventId l'identifiant unique de l'événement.
     * @param userId l'entité de l'utilisateur à inviter.
     * @return un DTO contenant les informations mises à jour de l'événement.
     * @throws EntityNotFoundException si l'événement n'existe pas en base de données.
     * @throws EventConflictException si l'utilisateur n'est pas disponible sur ce créneau (conflit d'agenda).
     */
    public EventResponseDto inviteUserToMeeting(Long eventId, Long userId){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (checkConflictWithAnotherEvent(user, event)) {
            throw new EventConflictException("Conflit d'agenda : " + user.getName() + " n'est pas disponible sur ce créneau.");
        }

        event.addParticipant(user, null);

        Event saved = eventRepository.save(event);
        return mapToResponse(saved);
    }

    /**
     * Invite un groupe d'utilisateurs à un événement.
     * <p>
     * Vérifie pour chaque utilisateur la disponibilité sur le créneau de l'événement.
     * Si un seul utilisateur présente un conflit, l'ensemble du processus est interrompu pour le groupe.
     * Les utilisateurs sans conflit sont ajoutés avec un statut "en attente" (null).
     *
     * @param eventId l'identifiant unique de l'événement.
     * @param userIds l'ensemble (Set) des utilisateurs à inviter.
     * @return un DTO contenant les informations mises à jour de l'événement.
     * @throws EntityNotFoundException si l'événement n'existe pas en base de données.
     * @throws EventConflictException si au moins un des utilisateurs présente un conflit d'agenda.
     */
    public EventResponseDto inviteUsersToMeeting(Long eventId, Set<Long> userIds) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        for (Long userId : userIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

            if (checkConflictWithAnotherEvent(user, event)) {
                throw new EventConflictException("Impossible d'inviter le groupe. Conflit d'agenda pour : " + user.getName());
            }
            event.addParticipant(user, null);
        }

        Event saved = eventRepository.save(event);
        return mapToResponse(saved);
    }

    /**
     * Traite la réponse d'un utilisateur à une invitation pour un événement.
     * <p>
     * Met à jour le statut d'acceptation de l'utilisateur. En cas de refus, une raison peut être fournie.
     * Si l'utilisateur accepte l'invitation, l'éventuelle raison précédemment enregistrée est effacée.
     *
     * @param eventId l'identifiant de l'événement concerné.
     * @param response la réponse de l'utilisateur (true pour accepté, false pour refusé).
     * @param email l'adresse email de l'utilisateur répondant à l'invitation.
     * @param raison la raison fournie en cas de refus (peut être null si accepté).
     * @return un DTO représentant l'invitation mise à jour pour cet utilisateur.
     * @throws EntityNotFoundException si l'événement n'existe pas ou si l'utilisateur n'y est pas invité.
     */
    public EventInvitationResponseDto responseToMeetingInvitation(Long eventId, boolean response, String email, String raison) {
        User user = userService.findByEmail(email);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        EventParticipant participantParticipant = event.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("L'utilisateur n'est pas invité à cet événement"));

        participantParticipant.setHasAccepted(response);
        participantParticipant.setReason(!response ? raison : null);

        // Si la personne refuse, on enregistre la raison.
        // Si elle accepte, on s'assure que la raison reste nulle.
        if (!response) {
            participantParticipant.setReason(raison);
        }

        Event saved = eventRepository.save(event);

        return mapToInvitationResponse(saved, user);
    }

    /**
     * Récupère la liste des invitations en attente pour un utilisateur donné.
     *
     * @param email l'adresse email de l'utilisateur ciblé.
     * @return une liste de DTOs représentant les invitations en attente pour cet utilisateur.
     */
    public List<EventInvitationResponseDto> getInvitationsToEvent(String email) {
        User user = userService.findByEmail(email);

        List<Event> events = eventRepository.findPendingInvitationsByUserId(user.getId());

        return events.stream()
                .map(event -> mapToInvitationResponse(event, user))
                .toList();
    }

    /**
     * Récupère le statut de réponse de tous les participants pour un événement spécifique.
     * <p>
     * Cette action est strictement réservée au créateur de l'événement.
     *
     * @param eventId l'identifiant de l'événement.
     * @param email l'adresse email de l'utilisateur effectuant la requête (doit être le créateur).
     * @return une liste de DTOs contenant le statut détaillé (présent, rôle, raison) de chaque participant.
     * @throws EntityNotFoundException si l'événement n'existe pas.
     * @throws IllegalArgumentException si l'utilisateur requérant n'est pas le créateur de l'événement.
     */
    public List<StatutInvitationParticipantDto> getEventInvitationStatut(Long eventId, String email) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        User creator = userService.findByEmail(email);

        if (event.getCreatedBy().getId() != creator.getId()) {
            throw new IllegalArgumentException("Pas le créateur de l'évènement");
        }


        return event.getParticipants().stream()
                .map(participant -> {
                    User user = participant.getUser();

                    return StatutInvitationParticipantDto.builder()
                            .participantName(user.getName())
                            .participantRole(user.getDisplayRole())
                            .isPresent(participant.getHasAccepted())
                            .reason(participant.getReason())
                            .build();
                })
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
                .map(g -> EventResponseDto.ProjectGroupRefDto.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .categoryName(g.getCategory().getName())
                        .parentGroupName(g.getParentGroup() != null ? g.getParentGroup().getName() : null)
                        .build())
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
                .participantNames(event.getParticipants().stream()
                        .map(participant -> participant.getUser().getName())
                        .collect(Collectors.toSet()))
                .build();
    }

    /**
     * Méthode utilitaire pour convertir un événement et un utilisateur ciblé en DTO de réponse d'invitation.
     *
     * @param event l'événement à mapper.
     * @param user l'utilisateur concerné par l'invitation.
     * @return un DTO formaté pour l'affichage des détails de l'invitation.
     */
    private EventInvitationResponseDto mapToInvitationResponse(Event event, User user) {
        return EventInvitationResponseDto.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .categoryType(event.getCategoryType())
                .categoryName(event.getCategory().getName())
                .createdBy(event.getCreatedBy().getName())
                .createdAt(event.getCreatedAt())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .participantName(user.getName())
                .participantId(user.getId())
                .build();
    }

    /**
     * Vérifie si un utilisateur possède déjà un événement accepté ou en attente sur le même créneau horaire.
     * <p>
     * Interroge la base de données pour compter les événements qui chevauchent la période
     * de l'événement fourni en paramètre.
     *
     * @param user l'utilisateur dont l'agenda doit être vérifié.
     * @param event l'événement contenant les dates de début et de fin à tester.
     * @return true si un conflit d'agenda est détecté, false sinon (ou si les dates de l'événement sont nulles).
     */
    private boolean checkConflictWithAnotherEvent(User user, Event event) {
        if (event.getStartDate() == null || event.getEndDate() == null) {
            return false;
        }
        long conflits = eventRepository.countConflictingEvents(
                user.getId(),
                event.getId(),
                event.getStartDate(),
                event.getEndDate()
        );
        return conflits > 0;
    }
}
