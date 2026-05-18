package fr.horizon.backend_api.controller;
import fr.horizon.backend_api.model.dto.*;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
/**
 * Contrôleur REST de gestion des événements académiques.
 * <p>
 * Expose les endpoints permettant de créer un événement et de
 * lister les événements par type (AUDIT ou DEFENSE).
 * <p>
 * La création est réservée aux utilisateurs ayant le rôle TEACHER ou ADMIN.
 */
public class EventController {
    private final EventService eventService;

    @Autowired
    private EventRepository repository;

    // Récupérer tous les événements (ou filtrer par mois idéalement)
    @GetMapping
    public List<Event> getAllEvents() {
        return repository.findAll();
    }

    // Ajouter un événement
    /**
     * Crée un nouvel événement académique.
     * <p>
     * L’email de l’utilisateur connecté est extrait du contexte
     * d’authentification Spring Security et transmis au service.
     *
     * @param request        données de création (nom, description, categoryId)
     * @param authentication contexte d’authentification Spring Security
     * @return événement créé sous forme de DTO avec statut HTTP 201
     */
// 1. Endpoint pour la création d'un événement standard
    @PostMapping
    public ResponseEntity<EventResponseDto> create(
            @RequestBody EventRequestDto request,
            Authentication authentication) {

        String email = authentication.getName();
        EventResponseDto response = eventService.create(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. Endpoint spécifique pour la création d'un événement lié à une école
    @PostMapping("/school")
    public ResponseEntity<EventResponseDto> createSchoolEvent(
            @RequestBody EventRequestDto request,
            Authentication authentication) {

        String email = authentication.getName();
        // Si la logique de création pour "school" diffère, vous pouvez appeler
        // une méthode dédiée dans votre service, par exemple : eventService.createSchoolEvent(...)
        EventResponseDto response = eventService.createSchoolEvent(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupère la liste des événements filtrés par type.
     *
     * @param type type d’événement ({@code AUDIT} ou {@code DEFENSE})
     * @return liste des événements correspondants, triés par date décroissante
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<EventResponseDto>> listByType(@PathVariable CategoryType type) {
        List<EventResponseDto> events = eventService.listByType(type);
        return ResponseEntity.ok(events);
    }

    /**
     * Invite un seul utilisateur à un événement spécifique.
     */
    @PostMapping("/{eventId}/invitations")
    public ResponseEntity<EventResponseDto> inviteUserToEvent(
            @PathVariable Long eventId,
            @RequestBody InvitationRequestDto invitationRequestDto) {

        EventResponseDto response = eventService.inviteUserToMeeting(eventId, invitationRequestDto.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * Invite un groupe d'utilisateurs à un événement spécifique.
     */
    @PostMapping("/{eventId}/invitations/bulk")
    public ResponseEntity<EventResponseDto> inviteUsersToEvent(
            @PathVariable Long eventId,
            @RequestBody Set<Long> userIds) {

        EventResponseDto response = eventService.inviteUsersToMeeting(eventId, userIds);
        return ResponseEntity.ok(response);
    }

    /**
     * Permet à un utilisateur de répondre à une invitation.
     */
    @PutMapping("/{eventId}/invitations/respond")
    public ResponseEntity<EventInvitationResponseDto> respondToInvitation(
            @PathVariable Long eventId,
            @RequestBody InvitationResponseRequestDto request,
            Authentication authentication) {

        EventInvitationResponseDto response = eventService.responseToMeetingInvitation(
                eventId,
                request.getResponse(),
                authentication.getName(),
                request.getRaison()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Récupère le statut de toutes les invitations pour un événement.
     */
    @GetMapping("/{eventId}/invitations/status")
    public ResponseEntity<List<StatutInvitationParticipantDto>> getEventInvitationStatus(
            @PathVariable Long eventId,
            Authentication authentication) {

        List<StatutInvitationParticipantDto> statusList = eventService.getEventInvitationStatut(
                eventId,
                authentication.getName()
        );

        return ResponseEntity.ok(statusList);
    }

    /**
     * Récupère la liste de toutes les invitations en attente de l'utilisateur connecté.
     */
    @GetMapping("/invitations/me")
    public ResponseEntity<List<EventInvitationResponseDto>> getMyInvitations(Authentication authentication) {
        List<EventInvitationResponseDto> response = eventService.getInvitationsToEvent(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public void deleteEvent(@PathVariable Long id) {
        eventService.deleteById(id);
    }

    /**
     * Crée un nouvel événement académique.
     * <p>
     * L’email de l’utilisateur connecté est extrait du contexte
     * d’authentification Spring Security et transmis au service.
     *
     * @param request        données de création (nom, description, categoryId)
     * @param authentication contexte d’authentification Spring Security
     * @return événement créé sous forme de DTO avec statut HTTP 201
     */
    @PostMapping("/meeting/{issueId}")
    public ResponseEntity<EventResponseDto> createMeeting(@RequestBody EventRequestDto request, Authentication authentication, @PathVariable Long issueId) {
        String email = authentication.getName();
        EventResponseDto response = eventService.createMeeting(request, email, issueId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Vérifie si une issue possède une réunion à venir.
     * GET /events/meeting/{issueId}/upcoming
     *
     * @param issueId l'identifiant du ticket
     * @return true si une réunion est prévue, false sinon
     */
    @GetMapping("/meeting/{issueId}/upcoming")
    public ResponseEntity<Boolean> checkUpcomingMeeting(@PathVariable Long issueId) {
        boolean hasMeeting = eventService.issueGotMeeting(issueId);
        return ResponseEntity.ok(hasMeeting);
    }

    /**
     * Récupère la liste de tous les événements créés par l'utilisateur connecté.
     */
    @GetMapping("/me/created")
    public ResponseEntity<List<EventResponseDto>> getMyCreatedEvents(Authentication authentication) {
        List<EventResponseDto> response = eventService.getMyCreatedEvents(authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Met à jour l'horaire d'un meeting suite à un refus/changement et gère les réinvitations.
     * PUT /events/{eventId}/reschedule?addAll=true
     */
    @PutMapping("/{eventId}/reschedule")
    public ResponseEntity<EventResponseDto> updateMeetingDueToChange(
            @PathVariable Long eventId,
            @RequestParam boolean addAll,
            @RequestBody UpdateMeetingDatesRequestDto datesRequest) {

        EventResponseDto response = eventService.updateMeetingDueToChange(
                eventId,
                datesRequest.getStartDate(),
                datesRequest.getEndDate(),
                addAll
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Annule un événement académique (réservé au créateur ou admin selon tes règles).
     * DELETE /events/{eventId}/cancel
     */
    @DeleteMapping("/{eventId}/cancel")
    public ResponseEntity<Void> cancelEvent(@PathVariable Long eventId) {
        eventService.cancelEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Soumet une demande de réunion pour un ticket (initiée par un étudiant).
     * POST /events/meeting/ask/{issueId}
     */
    @PostMapping("/meeting/ask/{issueId}")
    public ResponseEntity<EventResponseDto> askForMeeting(
            @RequestBody EventRequestDto request,
            Authentication authentication,
            @PathVariable Long issueId) {

        String email = authentication.getName();
        EventResponseDto response = eventService.askForMeeting(request, email, issueId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupère le statut de toutes les demandes de réunions initiées par l'utilisateur connecté.
     * GET /events/me/demands
     */
    @GetMapping("/me/demands")
    public ResponseEntity<List<MeetingDemandeDto>> getMyDemandsStatus(Authentication authentication) {
        List<MeetingDemandeDto> response = eventService.getMyDemandsStatus(authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Retire un participant spécifique d'une réunion.
     * DELETE /events/{eventId}/participants/{userId}
     *
     * @param eventId l'identifiant de la réunion
     * @param userId l'identifiant de l'utilisateur à retirer
     * @return le DTO de l'événement mis à jour
     */
    @DeleteMapping("/{eventId}/participants/{userId}")
    public ResponseEntity<EventResponseDto> deleteParticipantFromMeeting(
            @PathVariable Long eventId,
            @PathVariable Long userId) {

        EventResponseDto response = eventService.deleteParticipantFromMeeting(eventId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère tous les abonnés du ticket lié à un événement qui ne sont pas invités.
     * GET /events/{eventId}/issue-followers
     */
    @GetMapping("/{eventId}/possible-participants")
    public ResponseEntity<List<FollowersInfoDto>> getPossibleUserInterested(@PathVariable Long eventId) {
        List<FollowersInfoDto> followers = eventService.getPossibleUserInterested(eventId);
        return ResponseEntity.ok(followers);
    }



    /**
     * Endpoint pour peupler le FullCalendar côté Front-end.
     * Retourne tous les événements où l'utilisateur est impliqué.
     * GET /events/calendar
     */
    @GetMapping("/calendar")
    public ResponseEntity<List<EventResponseDto>> getMyCalendarEvents(Authentication authentication) {
        String email = authentication.getName();
        List<EventResponseDto> calendarEvents = eventService.getMyCalendarEvents(email);
        return ResponseEntity.ok(calendarEvents);
    }
}

