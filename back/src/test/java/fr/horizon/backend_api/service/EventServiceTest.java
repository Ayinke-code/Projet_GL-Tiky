package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Event;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.EventRequestDto;
import fr.horizon.backend_api.model.dto.EventResponseDto;
import fr.horizon.backend_api.model.enums.EventType;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private EventService eventService;

    // =============================================
    // create()
    // =============================================

    @Test
    void create_should_save_event_when_user_is_teacher() {

        Role teacherRole = new Role("TEACHER");

        User teacher = new User();
        teacher.setId(1L);
        teacher.setName("Sophie ROUSSEAU");
        teacher.setEmail("teacher@teacher.fr");
        teacher.setRoles(Set.of(teacherRole));

        Category category = new Category();
        category.setId(10L);
        category.setName("UI/UX");
        category.setEventType(EventType.AUDIT);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(10L);
        request.setName("Audit Sprint 3");
        request.setDescription("Revue du code du sprint 3");

        Event saved = Event.builder()
                .id(1L)
                .name("Audit Sprint 3")
                .description("Revue du code du sprint 3")
                .eventType(EventType.AUDIT)
                .category(category)
                .createdBy(teacher)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.findByEmail("teacher@teacher.fr")).thenReturn(teacher);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventResponseDto result = eventService.create(request, "teacher@teacher.fr");

        assertNotNull(result);
        assertEquals("Audit Sprint 3", result.getName());
        assertEquals("Revue du code du sprint 3", result.getDescription());
        assertEquals(EventType.AUDIT, result.getEventType());
        assertEquals("UI/UX", result.getCategoryName());
        assertEquals("Sophie ROUSSEAU", result.getCreatedBy());

        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void create_should_save_event_when_user_is_admin() {

        Role adminRole = new Role("ADMIN");

        User admin = new User();
        admin.setId(2L);
        admin.setName("Richard WOODWARD");
        admin.setEmail("admin@admin.fr");
        admin.setRoles(Set.of(adminRole));

        Category category = new Category();
        category.setId(20L);
        category.setName("PFE");
        category.setEventType(EventType.DEFENSE);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(20L);
        request.setName("Soutenance PFE - Groupe A");
        request.setDescription("Présentation du projet de fin d'études");

        Event saved = Event.builder()
                .id(2L)
                .name("Soutenance PFE - Groupe A")
                .description("Présentation du projet de fin d'études")
                .eventType(EventType.DEFENSE)
                .category(category)
                .createdBy(admin)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.findByEmail("admin@admin.fr")).thenReturn(admin);
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventResponseDto result = eventService.create(request, "admin@admin.fr");

        assertNotNull(result);
        assertEquals("Soutenance PFE - Groupe A", result.getName());
        assertEquals(EventType.DEFENSE, result.getEventType());
        assertEquals("PFE", result.getCategoryName());
        assertEquals("Richard WOODWARD", result.getCreatedBy());
    }

    @Test
    void create_should_throw_AccessDeniedException_when_student_creates_audit() {

        Role studentRole = new Role("STUDENT");

        User student = new User();
        student.setId(3L);
        student.setName("Raoudate");
        student.setEmail("student@student.fr");
        student.setRoles(Set.of(studentRole));

        Category category = new Category();
        category.setId(10L);
        category.setName("CI/CD");
        category.setEventType(EventType.AUDIT);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(10L);
        request.setName("Audit Pipeline");

        when(userService.findByEmail("student@student.fr")).thenReturn(student);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        assertThrows(AccessDeniedException.class,
                () -> eventService.create(request, "student@student.fr"));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void create_should_throw_AccessDeniedException_when_student_creates_defense() {

        Role studentRole = new Role("STUDENT");

        User student = new User();
        student.setId(3L);
        student.setName("Raoudate");
        student.setEmail("student@student.fr");
        student.setRoles(Set.of(studentRole));

        Category category = new Category();
        category.setId(20L);
        category.setName("Stage");
        category.setEventType(EventType.DEFENSE);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(20L);
        request.setName("Soutenance Stage");

        when(userService.findByEmail("student@student.fr")).thenReturn(student);
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));

        assertThrows(AccessDeniedException.class,
                () -> eventService.create(request, "student@student.fr"));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void create_should_throw_CategoryNotFoundException_when_category_not_found() {

        Role teacherRole = new Role("TEACHER");

        User teacher = new User();
        teacher.setId(1L);
        teacher.setEmail("teacher@teacher.fr");
        teacher.setRoles(Set.of(teacherRole));

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(999L);
        request.setName("Audit inconnu");

        when(userService.findByEmail("teacher@teacher.fr")).thenReturn(teacher);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class,
                () -> eventService.create(request, "teacher@teacher.fr"));
    }

    // =============================================
    // listByType()
    // =============================================

    @Test
    void listByType_should_return_list_of_dto() {

        User teacher = new User();
        teacher.setId(1L);
        teacher.setName("Sophie ROUSSEAU");

        Category category = new Category();
        category.setId(10L);
        category.setName("UI/UX");

        Event event = Event.builder()
                .id(1L)
                .name("Audit Sprint 1")
                .description("Revue sprint 1")
                .eventType(EventType.AUDIT)
                .category(category)
                .createdBy(teacher)
                .createdAt(LocalDateTime.now())
                .build();

        when(eventRepository.findByEventTypeOrderByCreatedAtDesc(EventType.AUDIT))
                .thenReturn(List.of(event));

        List<EventResponseDto> result = eventService.listByType(EventType.AUDIT);

        assertEquals(1, result.size());
        assertEquals("Audit Sprint 1", result.get(0).getName());
        assertEquals(EventType.AUDIT, result.get(0).getEventType());
        assertEquals("UI/UX", result.get(0).getCategoryName());
        assertEquals("Sophie ROUSSEAU", result.get(0).getCreatedBy());
    }

    @Test
    void listByType_should_return_empty_list_when_no_events() {

        when(eventRepository.findByEventTypeOrderByCreatedAtDesc(EventType.DEFENSE))
                .thenReturn(List.of());

        List<EventResponseDto> result = eventService.listByType(EventType.DEFENSE);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void create_should_save_event_when_category_has_no_event_type_restriction() {

        // Catégorie générale (eventType null) → pas de vérification de rôle
        Role studentRole = new Role("STUDENT");

        User student = new User();
        student.setId(3L);
        student.setName("Raoudate");
        student.setEmail("student@student.fr");
        student.setRoles(Set.of(studentRole));

        Category category = new Category();
        category.setId(30L);
        category.setName("Vie étudiante");
        category.setEventType(null);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(30L);
        request.setName("Réunion étudiante");
        request.setDescription("Réunion mensuelle");

        Event saved = Event.builder()
                .id(3L)
                .name("Réunion étudiante")
                .description("Réunion mensuelle")
                .eventType(null)
                .category(category)
                .createdBy(student)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.findByEmail("student@student.fr")).thenReturn(student);
        when(categoryRepository.findById(30L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventResponseDto result = eventService.create(request, "student@student.fr");

        assertNotNull(result);
        assertEquals("Réunion étudiante", result.getName());
        verify(eventRepository).save(any(Event.class));
    }
}
