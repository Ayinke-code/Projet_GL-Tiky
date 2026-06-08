package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.EventConflictException;
import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.*;
import fr.horizon.backend_api.model.dto.AvailabilityRequestDto;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Event;
import fr.horizon.backend_api.model.StudentGroup;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.model.enums.RoleEnum;
import fr.horizon.backend_api.repository.*;
import fr.horizon.backend_api.exception.CategoryNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import fr.horizon.backend_api.exception.EventNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.stream.Stream;

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
    private final IssueRepository issueRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final IssueFollowerRepository issueFollowerRepository;
    private final NotificationService notificationService;

    private static final String USER_NOT_FOUND = "User not found";
    private static final String ISSUE_NOT_FOUND = "Issue not found";
    private static final String EVENT_NOT_FOUND = "Event not found";
    private static final String WITH_ID = " with Id: ";

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
    @Transactional
    public EventResponseDto create(EventRequestDto request, String email) {
        User user = userService.findByEmail(email);
        Category category = getCategory(request.getCategoryId());

        validateTeacherAccess(user, category);
        validateEventDates(request, category);

        Event event = buildEvent(request, user, category);

        assignProjectGroups(event, request);
        validateGroupMembersAvailability(event);

        Event savedEvent = eventRepository.save(event);

        notifyProjectGroupMembers(savedEvent);

        return mapToResponse(savedEvent);
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
     * Crée un nouvel événement lié à une issue.
     * <p>
     * Vérifie que l’utilisateur possède le rôle requis avant la persistance.
     *
     * @param meetingRequest données de création de l’événement
     * @param email   email de l’utilisateur créateur, extrait du contexte de sécurité
     * @return événement créé sous forme de DTO
     * @throws AccessDeniedException si l’utilisateur n’a pas le rôle TEACHER ou ADMIN
     * @throws CategoryNotFoundException si la catégorie n’existe pas
     */
    public EventResponseDto createMeeting(EventRequestDto meetingRequest, String email, Long issueId){
            User user = userService.findByEmail(email);
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new EntityNotFoundException(ISSUE_NOT_FOUND));

        boolean hasTeacherRole = user.getRoles().stream()
                .anyMatch(r -> RoleEnum.TEACHER.value().equals(r.getName()) || RoleEnum.ADMIN.value().equals(r.getName()));
        if (!hasTeacherRole) {
            throw new AccessDeniedException("Seuls les enseignants peuvent faire une demande de meeting");
        }
        checkDatesForCreation(meetingRequest);

        Event event = Event.builder()
                .name(meetingRequest.getName())
                .description(meetingRequest.getDescription())
                .categoryType(CategoryType.ISSUE)
                .category(issue.getCategory())
                .createdBy(user)
                .validated(true)
                .issue(issue)
                .startDate(meetingRequest.getStartDate())
                .endDate(meetingRequest.getEndDate())
                .build();

        // Vérifier que le créateur n'a pas déjà un événement sur ce créneau
        if (checkConflictWithAnotherEvent(user, event)) {
            throw new EventConflictException("Conflit d'agenda : " + user.getName() + " n'est pas disponible sur ce créneau.");
        }

        // dire qu'il est présent
        event.addParticipant(user, true);
        Event saved = eventRepository.save(event);
        return mapToResponse(saved);
    }

    private void checkDatesForCreation(EventRequestDto meetingRequest) {
        if (meetingRequest.getStartDate() == null || meetingRequest.getEndDate() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires pour un événement de ce type.");
        }
        if(meetingRequest.getStartDate().isBefore(LocalDateTime.now().plusMinutes(30))){
            throw new IllegalArgumentException("La date de début doit être au moins 30 minutes dans le futur.");
        }
        if (!meetingRequest.getEndDate().isAfter(meetingRequest.getStartDate())) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");
        }
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
                .orElseThrow(() -> new EntityNotFoundException(EVENT_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        if (checkConflictWithAnotherEvent(user, event)) {
            throw new EventConflictException("Conflit d'agenda : " + user.getName() + " n'est pas disponible sur ce créneau.");
        }

        event.addParticipant(user, null);
        notificationService.inviteToEventNotification(user, event);

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
                .orElseThrow(() -> new EntityNotFoundException(EVENT_NOT_FOUND));

        for (Long userId : userIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND + WITH_ID + userId));

            if (checkConflictWithAnotherEvent(user, event)) {
                throw new EventConflictException("Impossible d'inviter le groupe. Conflit d'agenda pour : " + user.getName());
            }
            event.addParticipant(user, null);
            notificationService.inviteToEventNotification(user, event);
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
                .orElseThrow(() -> new EntityNotFoundException(EVENT_NOT_FOUND));

        EventParticipant participantParticipant = event.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("L'utilisateur n'est pas invité à cet événement"));

        participantParticipant.setHasAccepted(response);
        participantParticipant.setReason(!response ? raison : null);

        // Valider l'event UNIQUEMENT si le prof accepte.
        // En cas de refus, validated reste false → l'event disparaît du calendrier.
        if (response) {
            event.setValidated(true);
        }

        // Si la personne refuse, on enregistre la raison.
        // Si elle accepte, on s'assure que la raison reste nulle.
        if (!response) {
            participantParticipant.setReason(raison);
        }

        Event saved = eventRepository.save(event);
        notificationService.responseEventNotification(event.getCreatedBy(), event ,participantParticipant.getUser().getName(), response);

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
                .orElseThrow(() -> new EntityNotFoundException(EVENT_NOT_FOUND));

        User currentUser = userService.findByEmail(email);

        boolean isCreator = Objects.equals(event.getCreatedBy().getId(), currentUser.getId());
        boolean isManager = event.getIssue() != null && Objects.equals(event.getIssue().getManager().getId(), currentUser.getId());

        if (!isCreator && !isManager) {
            throw new IllegalArgumentException("Non autorisé à consulter les statuts");
        }

        return event.getParticipants().stream()
                .map(participant -> {
                    User user = participant.getUser();

                    return StatutInvitationParticipantDto.builder()
                            .participantId(user.getId())
                            .participantName(user.getName())
                            .participantRole(user.getDisplayRole())
                            .isPresent(participant.getHasAccepted())
                            .reason(participant.getReason())
                            .build();
                })
                .toList();
    }


    /**
     * Retourne les sessions assignées à l'étudiant connecté.
     * Inclut les événements liés aux groupes PROJECT, OPTION et PROMOTION
     * dont l'étudiant est membre (directement ou via la hiérarchie).
     *
     * @param email email de l'étudiant extrait du contexte de sécurité
     * @return liste de DTO triée par date de début croissante
     */
    @Transactional(readOnly = true)
    public List<EventResponseDto> listForStudent(String email) {
        User student = userService.findByEmail(email);

        // Tous les groupes dont l'étudiant est membre directement
        List<StudentGroup> directGroups = studentGroupRepository.findAllByMemberId(student.getId());

        Set<Long> groupIds = buildGroupIdHierarchy(directGroups);

        if (groupIds.isEmpty()) {
            return List.of();
        }

        return eventRepository.findByProjectGroupIds(groupIds)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retourne les sessions pour tous les rôles (STUDENT, TEACHER, ADMIN) :
     * - STUDENT : events des groupes dont il est membre + events ISSUE où il est participant/créateur
     * - TEACHER/ADMIN : events des groupes qu'il enseigne + events ISSUE où il est participant/créateur
     * Chaque event ISSUE inclut le statut de participation de l'utilisateur courant.
     */
    @Transactional(readOnly = true)
    public List<EventResponseDto> getMySessions(String email) {
        User user = userService.findByEmail(email);

        boolean isTeacherOrAdmin = user.getRoles().stream()
                .anyMatch(r -> RoleEnum.TEACHER.value().equals(r.getName()) || RoleEnum.ADMIN.value().equals(r.getName()));

        Map<Long, Event> eventMap = new LinkedHashMap<>();
        Map<Long, Boolean> participationStatus = new HashMap<>();

        if (isTeacherOrAdmin) {
            // Teacher/Admin : events AUDIT/DEFENSE des groupes qu'ils enseignent
            eventRepository.findGroupEventsForTeacher(user.getId())
                    .forEach(e -> eventMap.put(e.getId(), e));
        } else {
            // Student : events AUDIT/DEFENSE des groupes dont ils sont membres
            List<StudentGroup> directGroups = studentGroupRepository.findAllByMemberId(user.getId());
            Set<Long> groupIds = buildGroupIdHierarchy(directGroups);
            if (!groupIds.isEmpty()) {
                eventRepository.findByProjectGroupIds(groupIds)
                        .forEach(e -> eventMap.put(e.getId(), e));
            }
        }

        // Tous les rôles : events ISSUE (RDV) où l'utilisateur est créateur ou participant
        // Pour TEACHER/ADMIN : uniquement les RDV validés (accepted) dans "Mes sessions".
        // Les RDV en attente sont gérés dans "Gérer mon planning".
        eventRepository.findIssueEventsForUser(user.getId(), CategoryType.ISSUE)
                .forEach(e -> {
                    if (isTeacherOrAdmin && !Boolean.TRUE.equals(e.getValidated())) {
                        return; // RDV en attente : visible dans "Gérer mon planning", pas dans "Mes sessions"
                    }
                    eventMap.put(e.getId(), e);
                    participationStatus.put(e.getId(), resolveIssueStatus(e, user.getId()));
                });

        return eventMap.values().stream()
                .sorted(Comparator.comparing(
                        e -> e.getStartDate() != null ? e.getStartDate() : LocalDateTime.MAX,
                        Comparator.naturalOrder()
                ))
                .map(e -> {
                    EventResponseDto dto = mapToResponse(e);
                    if (participationStatus.containsKey(e.getId())) {
                        dto.setMyParticipationStatus(participationStatus.get(e.getId()));
                    }
                    return dto;
                })
                .toList();
    }

    /**
     * Calcule le statut de participation d'un utilisateur pour un event ISSUE :
     * - Si créateur : déduit le statut depuis la réponse des autres participants
     * - Si participant invité : retourne son propre hasAccepted
     */
    private Boolean resolveIssueStatus(Event event, Long userId) {
        if (event.getCreatedBy().getId().equals(userId)) {
            boolean anyRefused = event.getParticipants().stream()
                    .filter(p -> !p.getUser().getId().equals(userId))
                    .anyMatch(p -> Boolean.FALSE.equals(p.getHasAccepted()));
            if (anyRefused) return false;
            boolean anyAccepted = event.getParticipants().stream()
                    .filter(p -> !p.getUser().getId().equals(userId))
                    .anyMatch(p -> Boolean.TRUE.equals(p.getHasAccepted()));
            return anyAccepted ? true : null;
        }
        return event.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .map(EventParticipant::getHasAccepted)
                .orElse(null);
    }

    /** Construit la hiérarchie complète des groupes (PROJECT → OPTION parent → PROMOTION) pour un étudiant. */
    private Set<Long> buildGroupIdHierarchy(List<StudentGroup> directGroups) {
        Set<Long> groupIds = new HashSet<>();
        for (StudentGroup g : directGroups) {
            groupIds.add(g.getId());
            if (g.getParentGroup() != null) {
                groupIds.add(g.getParentGroup().getId());
                if (g.getParentGroup().getPromotion() != null) {
                    groupIds.add(g.getParentGroup().getPromotion().getId());
                }
            }
            if (g.getPromotion() != null) {
                groupIds.add(g.getPromotion().getId());
            }
        }
        return groupIds;
    }

    @Transactional
    public void deleteById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        eventRepository.delete(event);
    }

    /**
     * Récupère tous les événements consolidés pour le calendrier de l'utilisateur.
     *
     * @param email L'email de l'utilisateur connecté
     * @return Une liste de EventResponseDto formatée pour le frontend
     */
    @Transactional(readOnly = true)
    public List<EventResponseDto> getMyParticipations(String email) {
        User user = userService.findByEmail(email);
        return eventRepository.findAcceptedParticipationsByUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponseDto> getMyCalendarEvents(String email) {
        User user = userService.findByEmail(email);

        // Déduplication par ID : SELECT DISTINCT en JPQL peut remonter le même event plusieurs fois
        // si une entité satisfait plusieurs conditions JOIN (créateur + participant + membre groupe).
        List<EventResponseDto> regularEvents = eventRepository.findCalendarEventsForUser(user.getId()).stream()
                .filter(e -> {
                    // Pour les RDV (ISSUE) créés par cet utilisateur, ne pas afficher si un participant a refusé
                    if (CategoryType.ISSUE.equals(e.getCategoryType())
                            && e.getCreatedBy().getId().equals(user.getId())) {
                        return e.getParticipants().stream()
                                .filter(p -> !p.getUser().getId().equals(user.getId()))
                                .noneMatch(p -> Boolean.FALSE.equals(p.getHasAccepted()));
                    }
                    return true;
                })
                .map(this::mapToResponse)
                .collect(Collectors.toMap(
                        EventResponseDto::getId,
                        dto -> dto,
                        (a, b) -> a,          // garde le premier en cas de doublon
                        java.util.LinkedHashMap::new
                ))
                .values().stream()
                .collect(Collectors.toList());

        // Inclure les créneaux de disponibilité déclarés par l'utilisateur (validated=false, absents de findCalendarEventsForUser)
        List<EventResponseDto> availabilities = eventRepository
                .findByCreatedBy_IdAndCategoryTypeOrderByStartDateAsc(user.getId(), CategoryType.AVAILABILITY)
                .stream()
                .map(this::mapToResponse)
                .toList();

        return Stream.concat(regularEvents.stream(), availabilities.stream()).toList();
    }

    /**
     * Déclare une plage de disponibilité pour l'utilisateur connecté.
     * L'event créé est de type AVAILABILITY, validated=false, sans participants.
     *
     * @param request données de la disponibilité (startDate, endDate, description)
     * @param email   email de l'utilisateur connecté
     * @return le DTO de l'event créé
     */
    @Transactional
    public EventResponseDto createAvailability(AvailabilityRequestDto request, String email) {
        User user = userService.findByEmail(email);

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");
        }

        // Catégorie système dédiée aux disponibilités (créée une seule fois si absente)
        Category availabilityCategory = categoryRepository.findByNameIgnoreCase("__AVAILABILITY__")
                .orElseGet(() -> categoryRepository.save(
                        Category.builder()
                                .name("__AVAILABILITY__")
                                .subject("Disponibilité")
                                .categoryType(CategoryType.AVAILABILITY)
                                .build()
                ));

        Event event = Event.builder()
                .name("Disponible")
                .description(request.getDescription())
                .categoryType(CategoryType.AVAILABILITY)
                .category(availabilityCategory)
                .createdBy(user)
                .validated(false)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        return mapToResponse(eventRepository.save(event));
    }

    /**
     * Récupère toutes les disponibilités déclarées par l'utilisateur connecté.
     *
     * @param email email de l'utilisateur connecté
     * @return liste de DTOs triée par date de début croissante
     */
    public List<EventResponseDto> getMyAvailabilities(String email) {
        User user = userService.findByEmail(email);
        return eventRepository
                .findByCreatedBy_IdAndCategoryTypeOrderByStartDateAsc(user.getId(), CategoryType.AVAILABILITY)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Récupère toutes les disponibilités déclarées par un utilisateur donné.
     * Accessible pour vérifier la disponibilité avant d'envoyer une invitation.
     *
     * @param userId L'ID de l'utilisateur cible
     * @return liste de DTOs triée par date de début croissante
     */
    public List<EventResponseDto> getUserAvailabilities(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));
        return eventRepository
                .findByCreatedBy_IdAndCategoryTypeOrderByStartDateAsc(userId, CategoryType.AVAILABILITY)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Supprime une disponibilité appartenant à l'utilisateur connecté.
     *
     * @param id    identifiant de l'event AVAILABILITY
     * @param email email de l'utilisateur connecté
     * @throws EntityNotFoundException si l'event n'existe pas
     * @throws AccessDeniedException   si l'utilisateur n'est pas le créateur
     * @throws IllegalArgumentException si l'event n'est pas de type AVAILABILITY
     */
    @Transactional
    public void deleteAvailability(Long id, String email) {
        User user = userService.findByEmail(email);
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        if (!CategoryType.AVAILABILITY.equals(event.getCategoryType())) {
            throw new IllegalArgumentException("Cet événement n'est pas une disponibilité.");
        }
        if (!event.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres disponibilités.");
        }
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
                    Set<EventResponseDto.TeacherRefDto> teachers = (g.getParentGroup() != null
                            ? g.getParentGroup().getTeachers()
                            : g.getTeachers())
                            .stream()
                            .map(t -> EventResponseDto.TeacherRefDto.builder()
                                    .name(t.getName())
                                    .teacherType(t.getTeacherType() != null ? t.getTeacherType().name() : null)
                                    .build())
                            .collect(java.util.stream.Collectors.toSet());
                    return EventResponseDto.ProjectGroupRefDto.builder()
                            .id(g.getId())
                            .name(g.getName())
                            .categoryName(g.getCategory().getName())
                            .parentGroupName(g.getParentGroup() != null ? g.getParentGroup().getName() : null)
                            .teachers(teachers)
                            .build();
                })
                .toList();

        return EventResponseDto.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .categoryType(event.getCategoryType())
                .categoryName(event.getCategory() != null ? event.getCategory().getName() : null)
                .createdBy(event.getCreatedBy().getName())
                .createdAt(event.getCreatedAt())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .validated(event.getValidated())
                .projectGroups(groups)
                .participantNames(event.getParticipants().stream()
                        .map(participant -> participant.getUser().getName())
                        .collect(Collectors.toSet()))
                .participants(event.getParticipants().stream()
                        .map(p -> EventResponseDto.ParticipantInfoDto.builder()
                                .name(p.getUser().getName())
                                .hasAccepted(p.getHasAccepted())
                                .build())
                        .collect(Collectors.toList()))
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
                .categoryName(event.getCategory() != null ? event.getCategory().getName() : null)
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

    /**
     * Vérifie si un ticket possède au moins une réunion prévue dans le futur.
     *
     * @param issueId l'ID du ticket
     * @return true s'il y a une réunion à venir, false sinon
     */
    public boolean issueGotMeeting(Long issueId) {
        // Inclut les demandes en attente (validated=false) pour éviter les doublons
        return eventRepository.existsByIssueIdAndStartDateAfter(issueId, LocalDateTime.now());
    }

    /**
     * Récupère le statut des demandes de réunions initiées par un utilisateur (ex: étudiant).
     * * @param email l'adresse email de l'utilisateur initiateur de la demande.
     * @return Une liste de MeetingDemandeDto contenant l'ID de l'issue et le statut de validation.
     */
    public List<MeetingDemandeDto> getMyDemandsStatus(String email) {
        User user = userService.findByEmail(email);

        List<Event> myEvents = eventRepository.findByCreatedByIdOrderByCreatedAtDesc(user.getId());

        return myEvents.stream()
                .filter(event -> event.getIssue() != null)
                .map(event -> {
                    MeetingDemandeDto dto = new MeetingDemandeDto();
                    dto.setIssueId(event.getIssue().getId());
                    dto.setStatus(event.getValidated());
                    return dto;
                })
                .toList();
    }

    /**
     * Récupère la liste de tous les événements créés par l'utilisateur connecté.
     *
     * @param email l'adresse email de l'utilisateur connecté (extrait du token).
     * @return une liste de DTOs représentant les événements créés par l'utilisateur.
     */
    public List<EventResponseDto> getMyCreatedEvents(String email) {
        User user = userService.findByEmail(email);

        List<Event> myEvents = eventRepository.findEventsManagedOrCreatedByUser(user.getId());

        return myEvents.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Met à jour l'horaire d'une réunion suite à un refus/changement et gère la réinvitation des participants.
     * en vérifiant les conflits d'agenda pour chaque participant concerné.
     * @param eventId      L'identifiant de l'événement à modifier
     * @param newStartDate Le nouvel horaire de début
     * @param newEndDate   Le nouvel horaire de fin
     * @param addAll       true pour réinviter tout le monde, false pour ne garder que ceux qui avaient accepté
     * @return Le DTO de l'événement mis à jour
     */
    @Transactional
    public EventResponseDto updateMeetingDueToChange(
            Long eventId,
            LocalDateTime newStartDate,
            LocalDateTime newEndDate,
            boolean addAll
    ) {

        Event event = getEventOrThrow(eventId);

        validateDates(newStartDate, newEndDate);

        event.setStartDate(newStartDate);
        event.setEndDate(newEndDate);

        validateOrganizerAvailability(event);

        if (addAll) {
            updateParticipantsKeepingAll(event);
        } else {
            updateParticipantsRemovingUnavailable(event);
        }

        Event saved = eventRepository.save(event);

        if (saved.getParticipants() != null) {
            for (EventParticipant participant : saved.getParticipants()) {
                User targetUser = participant.getUser();

                if (targetUser != null && !targetUser.getId().equals(saved.getCreatedBy().getId())) {
                    notificationService.updateEventNotification(targetUser, saved);
                }
            }
        }

        return mapToResponse(saved);
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(EVENT_NOT_FOUND));
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {

        if (start == null || end == null) {
            throw new IllegalArgumentException(
                    "Les nouvelles dates de début et de fin sont obligatoires."
            );
        }

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException(
                    "La date de fin doit être postérieure à la date de début."
            );
        }
    }

    private void validateOrganizerAvailability(Event event) {

        if (checkConflictWithAnotherEvent(event.getCreatedBy(), event)) {
            throw new EventConflictException(
                    "Conflit d'agenda : L'organisateur n'est pas disponible sur ce nouveau créneau."
            );
        }
    }

    private void updateParticipantsKeepingAll(Event event) {

        for (EventParticipant participant : event.getParticipants()) {

            if (isOrganizer(event, participant)) {
                participant.setHasAccepted(true);
            } else {

                validateParticipantAvailability(participant, event);

                participant.setHasAccepted(null);
            }

            resetParticipantReason(participant);
        }
    }

    private void updateParticipantsRemovingUnavailable(Event event) {

        event.getParticipants().removeIf(participant ->
                !isOrganizer(event, participant)
                        && shouldRemoveParticipant(participant, event)
        );

        for (EventParticipant participant : event.getParticipants()) {

            participant.setHasAccepted(
                    isOrganizer(event, participant) ? true : null
            );

            resetParticipantReason(participant);
        }
    }

    private boolean shouldRemoveParticipant(EventParticipant participant, Event event) {

        return !Boolean.TRUE.equals(participant.getHasAccepted())
                || checkConflictWithAnotherEvent(participant.getUser(), event);
    }

    private void validateParticipantAvailability(
            EventParticipant participant,
            Event event
    ) {

        if (checkConflictWithAnotherEvent(participant.getUser(), event)) {
            throw new EventConflictException(
                    "Impossible de replanifier. Conflit d'agenda pour : "
                            + participant.getUser().getName()
            );
        }
    }

    private boolean isOrganizer(Event event, EventParticipant participant) {

        return participant.getUser().getId()
                .equals(event.getCreatedBy().getId());
    }

    private void resetParticipantReason(EventParticipant participant) {
        participant.setReason(null);
    }

    /**
     * Annule un événement en supprimant d'abord tous ses participants
     * puis en supprimant l'événement lui-même.
     *
     * @param eventId L'identifiant de l'événement à annuler
     */
    @Transactional
    public void cancelEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(EVENT_NOT_FOUND+WITH_ID + eventId));

        for (EventParticipant participant : event.getParticipants()) {
            User targetUser = participant.getUser();

            if (targetUser != null && !targetUser.getId().equals(event.getCreatedBy().getId())) {
                notificationService.cancelEventNotification(targetUser, event);
            }
        }

        // Enlever tous les participants (nettoie la table d'association)
        event.getParticipants().clear();

        // Supprimer définitivement l'événement
        eventRepository.delete(event);
    }

    /**
     * Crée une demande d'événement lié à une issue.
     * <p>
     * Vérifie que l’utilisateur possède le rôle requis avant la persistance.
     *
     * @param meetingRequest données de création de l’événement
     * @param email   email de l’utilisateur créateur, extrait du contexte de sécurité
     * @return événement créé sous forme de DTO
     * @throws AccessDeniedException si l’utilisateur n’a pas le rôle TEACHER ou ADMIN
     * @throws CategoryNotFoundException si la catégorie n’existe pas
     */
    public EventResponseDto askForMeeting(EventRequestDto meetingRequest, String email, long issueId){
        User user = userService.findByEmail(email);
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new EntityNotFoundException(ISSUE_NOT_FOUND));

        // Bloquer si une demande (en attente ou confirmée) existe déjà pour ce ticket
        if (eventRepository.existsByIssueIdAndStartDateAfter(issueId, LocalDateTime.now())) {
            throw new EventConflictException(
                "Une demande de rendez-vous est déjà en cours pour ce ticket."
            );
        }

        checkDatesForCreation(meetingRequest);

        Event event = Event.builder()
                .name(meetingRequest.getName())
                .description(meetingRequest.getDescription())
                .categoryType(CategoryType.ISSUE)
                .category(issue.getCategory())
                .createdBy(issue.getManager())
                .issue(issue)
                .validated(false)
                .startDate(meetingRequest.getStartDate())
                .endDate(meetingRequest.getEndDate())
                .build();

        event.addParticipant(user, true);
        Event saved = eventRepository.save(event);

        // Si c'est le manager (prof) qui crée → inviter le créateur du ticket (étudiant)
        // Si c'est l'étudiant qui crée → inviter le manager (prof)
        Long otherPartyId = user.getId().equals(issue.getManager().getId())
                ? issue.getCreatedBy().getId()
                : issue.getManager().getId();

        return inviteUserToMeeting(saved.getId(), otherPartyId);

    }

    /**
     * Retire un participant d'un event
     *
     * @param eventId L'identifiant de l'événement
     * @param userId L'identifiant de l'utilisateur a retirer
     * @return l'event DTO mis a jour
     */
    @Transactional
    public EventResponseDto deleteParticipantFromMeeting(Long eventId, Long userId){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(EVENT_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));


        event.deleteParticipant(user);

        Event saved = eventRepository.save(event);
        return mapToResponse(saved);
    }

    /**
     * Récupère la liste complète des abonnés (followers) du ticket auquel l'événement est rattaché.
     *
     * @param eventId L'identifiant de l'événement
     * @return La liste des utilisateurs abonnés au ticket
     */
    @Transactional(readOnly = true)
    public List<FollowersInfoDto> getPossibleUserInterested(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(EVENT_NOT_FOUND+WITH_ID + eventId));

        if (event.getIssue() == null) {
            return Collections.emptyList();
        }

        Set<Long> alreadyInvitedIds = event.getParticipants().stream()
                .map(participant -> participant.getUser().getId())
                .collect(Collectors.toSet());

        List<User> followers = issueFollowerRepository.findUsersByIssueId(event.getIssue().getId());

        return followers.stream()
                .filter(follower -> !alreadyInvitedIds.contains(follower.getId()))
                .map(this::toFollowerInfoDto)
                .toList();
    }

    private FollowersInfoDto toFollowerInfoDto(User user) {
        if (user == null) {
            return null;
        }

        return FollowersInfoDto.builder()
                .id(user.getId())
                .name(user.getName())
                .role(user.getDisplayRole())
                .build();
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    /**
     * Vérifie qu'aucun membre ni enseignant des groupes convoqués n'a de conflit d'agenda.
     * Lève une EventConflictException avec la liste des personnes en conflit.
     */
    private void validateGroupMembersAvailability(Event event) {
        if (event.getProjectGroups() == null || event.getProjectGroups().isEmpty()) {
            return;
        }
        if (event.getStartDate() == null || event.getEndDate() == null) {
            return;
        }

        List<String> conflicting = event.getProjectGroups().stream()
                .flatMap(group -> Stream.concat(
                        group.getMembers() != null ? group.getMembers().stream() : Stream.empty(),
                        group.getTeachers() != null ? group.getTeachers().stream() : Stream.empty()))
                .distinct()
                .filter(u -> eventRepository.countConflictingEvents(
                        u.getId(), null, event.getStartDate(), event.getEndDate()) > 0)
                .map(User::getName)
                .collect(Collectors.toList());

        if (!conflicting.isEmpty()) {
            throw new EventConflictException(
                "Conflit d'agenda détecté pour : " + String.join(", ", conflicting)
                + ". Ces personnes ont déjà un événement sur ce créneau.");
        }
    }

    /**
     * Associe uniquement les groupes de projet à l'événement.
     */
    private void assignProjectGroups(Event event, EventRequestDto request) {
        if (request.getProjectGroupIds() == null
                || request.getProjectGroupIds().isEmpty()
                || !isRestrictedCategory(event.getCategory())) {
            return;
        }

        Set<StudentGroup> groups = new HashSet<>(
                studentGroupRepository.findAllById(request.getProjectGroupIds()));

        event.setProjectGroups(groups);
    }

    /**
     * Notifie les membres des groupes de projet associés à l'événement sauvegardé.
     */
    private void notifyProjectGroupMembers(Event savedEvent) {
        if (savedEvent.getProjectGroups() == null || savedEvent.getProjectGroups().isEmpty()) {
            return;
        }

        Set<User> usersToNotify = savedEvent.getProjectGroups().stream()
                .flatMap(group -> Stream.concat(
                        group.getMembers() != null ? group.getMembers().stream() : Stream.empty(),
                        group.getTeachers() != null ? group.getTeachers().stream() : Stream.empty()))
                .collect(Collectors.toSet());

        usersToNotify.stream()
                .filter(user -> !user.getId().equals(savedEvent.getCreatedBy().getId()))
                .forEach(user -> notificationService.inviteToEventNotification(user, savedEvent));
    }

    private void validateEventDates(EventRequestDto request, Category category) {
        if (!isRestrictedCategory(category)) {
            return;
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException(
                    "Les dates de début et de fin sont obligatoires pour un événement de ce type.");
        }

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new IllegalArgumentException(
                    "La date de fin doit être postérieure à la date de début.");
        }
    }

    private Event buildEvent(EventRequestDto request, User user, Category category) {
        return Event.builder()
                .name(request.getName())
                .description(request.getDescription())
                .categoryType(category.getCategoryType())
                .category(category)
                .validated(true)
                .createdBy(user)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
    }

    private boolean isRestrictedCategory(Category category) {
        return category.getCategoryType() == CategoryType.AUDIT
                || category.getCategoryType() == CategoryType.DEFENSE;
    }

    private void validateTeacherAccess(User user, Category category) {
        if (!isRestrictedCategory(category)) {
            return;
        }

        boolean hasTeacherRole = user.getRoles().stream()
                .anyMatch(role ->
                        RoleEnum.TEACHER.value().equals(role.getName())
                                || RoleEnum.ADMIN.value().equals(role.getName()));

        if (!hasTeacherRole) {
            throw new AccessDeniedException(
                    "Seuls les enseignants peuvent créer des événements de type "
                            + category.getCategoryType());
        }
    }



}
