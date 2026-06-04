package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.EventConflictException;
import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.*;
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
                .validated(true)
                .createdBy(user)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        // Affectation des groupes projet (AUDIT / DEFENSE uniquement)
        if ((category.getCategoryType() == CategoryType.AUDIT || category.getCategoryType() == CategoryType.DEFENSE)
                && request.getProjectGroupIds() != null && !request.getProjectGroupIds().isEmpty()) {
            Set<StudentGroup> groups = new HashSet<>(studentGroupRepository.findAllById(request.getProjectGroupIds()));
            event.setProjectGroups(groups);

            Set<User> usersToNotify = new HashSet<>();
            for (StudentGroup group : groups) {
                if (group.getMembers() != null) {
                    usersToNotify.addAll(group.getMembers());
                }
                if (group.getTeachers() != null) {
                    usersToNotify.addAll(group.getTeachers());
                }
            }

            for (User targetUser : usersToNotify) {
                // ne pas notifier le créateur
                if (!targetUser.getId().equals(event.getCreatedBy().getId())) {
                    notificationService.inviteToEventNotification(targetUser, event);
                }
            }

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

        // dire qu'il est présent
        event.addParticipant(user, true);
        Event saved = eventRepository.save(event);
        return mapToResponse(saved);
    }

    private void checkDatesForCreation(EventRequestDto meetingRequest) {
        if (meetingRequest.getStartDate() == null || meetingRequest.getEndDate() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires pour un événement de ce type.");
        }
        if(meetingRequest.getStartDate().isBefore(LocalDateTime.now())){
            throw new IllegalArgumentException("La date de début doit être antérieur à la date courante.");
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

        if(response || Objects.equals(event.getIssue().getManager().getId(), user.getId())){
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
    public List<EventResponseDto> getMyCalendarEvents(String email) {
        User user = userService.findByEmail(email);

        List<Event> events = eventRepository.findCalendarEventsForUser(user.getId());

        return events.stream()
                .map(this::mapToResponse)
                .toList();
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

    /**
     * Vérifie si un ticket possède au moins une réunion prévue dans le futur.
     *
     * @param issueId l'ID du ticket
     * @return true s'il y a une réunion à venir, false sinon
     */
    public boolean issueGotMeeting(Long issueId) {
        return eventRepository.existsByIssueIdAndStartDateAfterAndValidatedTrue(issueId, LocalDateTime.now());
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

        checkDatesForCreation(meetingRequest);

        Event event = Event.builder()
                .name(meetingRequest.getName())
                .description(meetingRequest.getDescription())
                .categoryType(CategoryType.ISSUE)
                .category(issue.getCategory())
                .createdBy(user)
                .issue(issue)
                .validated(false)
                .startDate(meetingRequest.getStartDate())
                .endDate(meetingRequest.getEndDate())
                .build();

        event.addParticipant(user, true);
        Event saved = eventRepository.save(event);

        return inviteUserToMeeting(saved.getId(), issue.getManager().getId());

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


}
