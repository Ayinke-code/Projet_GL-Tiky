package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Event;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.EventRequestDto;
import fr.horizon.backend_api.model.dto.EventResponseDto;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.model.enums.TeacherType;
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
        category.setCategoryType(CategoryType.AUDIT);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(10L);
        request.setName("Audit Sprint 3");
        request.setDescription("Revue du code du sprint 3");
        request.setStartDate(LocalDateTime.of(2026, 6, 1, 9, 0));
        request.setEndDate(LocalDateTime.of(2026, 6, 1, 11, 0));

        Event saved = Event.builder()
                .id(1L)
                .name("Audit Sprint 3")
                .description("Revue du code du sprint 3")
                .categoryType(CategoryType.AUDIT)
                .category(category)
                .createdBy(teacher)
                .startDate(LocalDateTime.of(2026, 6, 1, 9, 0))
                .endDate(LocalDateTime.of(2026, 6, 1, 11, 0))
                .build();

        when(userService.findByEmail("teacher@teacher.fr")).thenReturn(teacher);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventResponseDto result = eventService.create(request, "teacher@teacher.fr");

        assertNotNull(result);
        assertEquals("Audit Sprint 3", result.getName());
        assertEquals("Revue du code du sprint 3", result.getDescription());
        assertEquals(CategoryType.AUDIT, result.getCategoryType());
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
        category.setCategoryType(CategoryType.DEFENSE);

        LocalDateTime defenseStart = LocalDateTime.of(2026, 7, 1, 14, 0);
        LocalDateTime defenseEnd   = LocalDateTime.of(2026, 7, 1, 16, 0);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(20L);
        request.setName("Soutenance PFE - Groupe A");
        request.setDescription("Présentation du projet de fin d'études");
        request.setStartDate(defenseStart);
        request.setEndDate(defenseEnd);

        Event saved = Event.builder()
                .id(2L)
                .name("Soutenance PFE - Groupe A")
                .description("Présentation du projet de fin d'études")
                .categoryType(CategoryType.DEFENSE)
                .category(category)
                .createdBy(admin)
                .startDate(defenseStart)
                .endDate(defenseEnd)
                .build();

        when(userService.findByEmail("admin@admin.fr")).thenReturn(admin);
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventResponseDto result = eventService.create(request, "admin@admin.fr");

        assertNotNull(result);
        assertEquals("Soutenance PFE - Groupe A", result.getName());
        assertEquals(CategoryType.DEFENSE, result.getCategoryType());
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
        category.setCategoryType(CategoryType.AUDIT);

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
        category.setCategoryType(CategoryType.DEFENSE);

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
                .categoryType(CategoryType.AUDIT)
                .category(category)
                .createdBy(teacher)
                .build();

        when(eventRepository.findByCategoryTypeOrderByCreatedAtDesc(CategoryType.AUDIT))
                .thenReturn(List.of(event));

        List<EventResponseDto> result = eventService.listByType(CategoryType.AUDIT);

        assertEquals(1, result.size());
        assertEquals("Audit Sprint 1", result.get(0).getName());
        assertEquals(CategoryType.AUDIT, result.get(0).getCategoryType());
        assertEquals("UI/UX", result.get(0).getCategoryName());
        assertEquals("Sophie ROUSSEAU", result.get(0).getCreatedBy());
    }

    @Test
    void listByType_should_return_empty_list_when_no_events() {

        when(eventRepository.findByCategoryTypeOrderByCreatedAtDesc(CategoryType.DEFENSE))
                .thenReturn(List.of());

        List<EventResponseDto> result = eventService.listByType(CategoryType.DEFENSE);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void create_should_save_event_when_category_has_no_event_type_restriction() {

        // Catégorie générale (categoryType null) → pas de vérification de rôle
        Role studentRole = new Role("STUDENT");

        User student = new User();
        student.setId(3L);
        student.setName("Raoudate");
        student.setEmail("student@student.fr");
        student.setRoles(Set.of(studentRole));

        Category category = new Category();
        category.setId(30L);
        category.setName("Vie étudiante");
        category.setCategoryType(null);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(30L);
        request.setName("Réunion étudiante");
        request.setDescription("Réunion mensuelle");

        Event saved = Event.builder()
                .id(3L)
                .name("Réunion étudiante")
                .description("Réunion mensuelle")
                .categoryType(null)
                .category(category)
                .createdBy(student)
                .build();

        when(userService.findByEmail("student@student.fr")).thenReturn(student);
        when(categoryRepository.findById(30L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventResponseDto result = eventService.create(request, "student@student.fr");

        assertNotNull(result);
        assertEquals("Réunion étudiante", result.getName());
        verify(eventRepository).save(any(Event.class));
    }

    // =============================================
    // create() — validation des dates (AUDIT)
    // =============================================

    @Test
    void create_should_save_audit_when_endDate_is_after_startDate() {

        Role teacherRole = new Role("TEACHER");

        User teacher = new User();
        teacher.setId(1L);
        teacher.setName("Sophie ROUSSEAU");
        teacher.setEmail("teacher@teacher.fr");
        teacher.setRoles(Set.of(teacherRole));

        Category category = new Category();
        category.setId(10L);
        category.setName("UI/UX");
        category.setCategoryType(CategoryType.AUDIT);

        LocalDateTime start = LocalDateTime.of(2026, 6, 10, 9, 0);
        LocalDateTime end   = LocalDateTime.of(2026, 6, 10, 11, 0);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(10L);
        request.setName("Audit Sprint 4");
        request.setStartDate(start);
        request.setEndDate(end);

        Event saved = Event.builder()
                .id(5L)
                .name("Audit Sprint 4")
                .categoryType(CategoryType.AUDIT)
                .category(category)
                .createdBy(teacher)
                .startDate(start)
                .endDate(end)
                .build();

        when(userService.findByEmail("teacher@teacher.fr")).thenReturn(teacher);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventResponseDto result = eventService.create(request, "teacher@teacher.fr");

        assertNotNull(result);
        assertEquals(start, result.getStartDate());
        assertEquals(end, result.getEndDate());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void create_should_throw_IllegalArgumentException_when_endDate_not_after_startDate() {

        Role teacherRole = new Role("TEACHER");

        User teacher = new User();
        teacher.setId(1L);
        teacher.setEmail("teacher@teacher.fr");
        teacher.setRoles(Set.of(teacherRole));

        Category category = new Category();
        category.setId(10L);
        category.setCategoryType(CategoryType.AUDIT);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(10L);
        request.setName("Audit invalide");
        request.setStartDate(LocalDateTime.of(2026, 6, 10, 11, 0));
        request.setEndDate(LocalDateTime.of(2026, 6, 10, 9, 0)); // fin avant début

        when(userService.findByEmail("teacher@teacher.fr")).thenReturn(teacher);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        assertThrows(IllegalArgumentException.class,
                () -> eventService.create(request, "teacher@teacher.fr"));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void create_should_throw_IllegalArgumentException_when_dates_missing_for_audit() {

        Role teacherRole = new Role("TEACHER");

        User teacher = new User();
        teacher.setId(1L);
        teacher.setEmail("teacher@teacher.fr");
        teacher.setRoles(Set.of(teacherRole));

        Category category = new Category();
        category.setId(10L);
        category.setCategoryType(CategoryType.AUDIT);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(10L);
        request.setName("Audit sans dates");
        // startDate et endDate non renseignées

        when(userService.findByEmail("teacher@teacher.fr")).thenReturn(teacher);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        assertThrows(IllegalArgumentException.class,
                () -> eventService.create(request, "teacher@teacher.fr"));

        verify(eventRepository, never()).save(any());
    }

    // =============================================
    // create() — Option Leader
    // =============================================

    @Test
    void create_should_save_audit_when_user_is_option_leader() {

        Role teacherRole = new Role("TEACHER");

        User optionLeader = new User();
        optionLeader.setId(2L);
        optionLeader.setName("Paul MARTIN");
        optionLeader.setEmail("teacher2@teacher.fr");
        optionLeader.setRoles(Set.of(teacherRole));
        optionLeader.setTeacherType(TeacherType.OPTION_LEADER);

        Category category = new Category();
        category.setId(10L);
        category.setName("UI/UX");
        category.setCategoryType(CategoryType.AUDIT);

        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 9, 0);
        LocalDateTime end   = LocalDateTime.of(2026, 6, 1, 11, 0);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(10L);
        request.setName("Audit Sprint 3 — Option");
        request.setDescription("Revue du code du sprint 3 par l'option leader");
        request.setStartDate(start);
        request.setEndDate(end);

        Event saved = Event.builder()
                .id(10L)
                .name("Audit Sprint 3 — Option")
                .description("Revue du code du sprint 3 par l'option leader")
                .categoryType(CategoryType.AUDIT)
                .category(category)
                .createdBy(optionLeader)
                .startDate(start)
                .endDate(end)
                .build();

        when(userService.findByEmail("teacher2@teacher.fr")).thenReturn(optionLeader);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventResponseDto result = eventService.create(request, "teacher2@teacher.fr");

        assertNotNull(result);
        assertEquals("Audit Sprint 3 — Option", result.getName());
        assertEquals("Revue du code du sprint 3 par l'option leader", result.getDescription());
        assertEquals(CategoryType.AUDIT, result.getCategoryType());
        assertEquals("UI/UX", result.getCategoryName());
        assertEquals("Paul MARTIN", result.getCreatedBy());

        verify(eventRepository).save(any(Event.class));
    }

    // =============================================
    // create() — validation des dates (DEFENSE)
    // =============================================

    @Test
    void create_should_save_defense_when_endDate_is_after_startDate() {

        Role teacherRole = new Role("TEACHER");

        User teacher = new User();
        teacher.setId(1L);
        teacher.setName("Sophie ROUSSEAU");
        teacher.setEmail("teacher@teacher.fr");
        teacher.setRoles(Set.of(teacherRole));

        Category category = new Category();
        category.setId(20L);
        category.setName("PFE");
        category.setCategoryType(CategoryType.DEFENSE);

        LocalDateTime start = LocalDateTime.of(2026, 7, 15, 10, 0);
        LocalDateTime end   = LocalDateTime.of(2026, 7, 15, 12, 0);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(20L);
        request.setName("Soutenance PFE - Groupe B");
        request.setStartDate(start);
        request.setEndDate(end);

        Event saved = Event.builder()
                .id(6L)
                .name("Soutenance PFE - Groupe B")
                .categoryType(CategoryType.DEFENSE)
                .category(category)
                .createdBy(teacher)
                .startDate(start)
                .endDate(end)
                .build();

        when(userService.findByEmail("teacher@teacher.fr")).thenReturn(teacher);
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventResponseDto result = eventService.create(request, "teacher@teacher.fr");

        assertNotNull(result);
        assertEquals(start, result.getStartDate());
        assertEquals(end, result.getEndDate());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void create_should_throw_IllegalArgumentException_when_endDate_not_after_startDate_for_defense() {

        Role teacherRole = new Role("TEACHER");

        User teacher = new User();
        teacher.setId(1L);
        teacher.setEmail("teacher@teacher.fr");
        teacher.setRoles(Set.of(teacherRole));

        Category category = new Category();
        category.setId(20L);
        category.setCategoryType(CategoryType.DEFENSE);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(20L);
        request.setName("Soutenance invalide");
        request.setStartDate(LocalDateTime.of(2026, 7, 15, 12, 0));
        request.setEndDate(LocalDateTime.of(2026, 7, 15, 10, 0)); // fin avant début

        when(userService.findByEmail("teacher@teacher.fr")).thenReturn(teacher);
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));

        assertThrows(IllegalArgumentException.class,
                () -> eventService.create(request, "teacher@teacher.fr"));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void create_should_throw_IllegalArgumentException_when_dates_missing_for_defense() {

        Role teacherRole = new Role("TEACHER");

        User teacher = new User();
        teacher.setId(1L);
        teacher.setEmail("teacher@teacher.fr");
        teacher.setRoles(Set.of(teacherRole));

        Category category = new Category();
        category.setId(20L);
        category.setCategoryType(CategoryType.DEFENSE);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(20L);
        request.setName("Soutenance sans dates");
        // startDate et endDate non renseignées

        when(userService.findByEmail("teacher@teacher.fr")).thenReturn(teacher);
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));

        assertThrows(IllegalArgumentException.class,
                () -> eventService.create(request, "teacher@teacher.fr"));

        verify(eventRepository, never()).save(any());
    }
}
