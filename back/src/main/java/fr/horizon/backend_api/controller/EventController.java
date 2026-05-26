package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.User;
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
    @PostMapping
    public ResponseEntity<EventResponseDto> create(@RequestBody EventRequestDto request, Authentication authentication) {
        String email = authentication.getName();
        EventResponseDto response = eventService.create(request, email);
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
            @RequestBody Long userId) {

        EventResponseDto response = eventService.inviteUserToMeeting(eventId, userId);
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
}

