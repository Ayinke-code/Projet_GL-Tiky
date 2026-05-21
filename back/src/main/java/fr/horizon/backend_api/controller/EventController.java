package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.EventRequestDto;
import fr.horizon.backend_api.model.dto.EventResponseDto;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.model.enums.TeacherType;
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
}

