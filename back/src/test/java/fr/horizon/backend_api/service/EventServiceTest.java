package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.exception.EventNotFoundException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Event;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.StudentGroup;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.EventRequestDto;
import fr.horizon.backend_api.model.dto.EventResponseDto;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.model.enums.GroupType;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.EventRepository;
import fr.horizon.backend_api.repository.StudentGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.HashSet;
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

    @Mock
    private StudentGroupRepository studentGroupRepository;

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

        when(eventRepository.findByCategoryTypeWithGroups(CategoryType.AUDIT))
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

        when(eventRepository.findByCategoryTypeWithGroups(CategoryType.DEFENSE))
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

    // =============================================
    // create() — projectGroupIds
    // =============================================

    @Test
    void create_should_assign_project_groups_when_AUDIT_and_projectGroupIds_provided() {

        Role teacherRole = new Role("TEACHER");

        User teacher = new User();
        teacher.setId(1L);
        teacher.setName("Sophie ROUSSEAU");
        teacher.setEmail("teacher@teacher.fr");
        teacher.setRoles(Set.of(teacherRole));

        Category eventCategory = new Category();
        eventCategory.setId(10L);
        eventCategory.setName("UI/UX");
        eventCategory.setCategoryType(CategoryType.AUDIT);

        Category groupCategory = new Category();
        groupCategory.setId(20L);
        groupCategory.setName("Projet Horizon");

        StudentGroup projectGroup = StudentGroup.builder()
                .id(5L)
                .name("Projet Alpha")
                .type(GroupType.PROJECT)
                .category(groupCategory)
                .createdBy(teacher)
                .members(new HashSet<>())
                .build();

        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 9, 0);
        LocalDateTime end   = LocalDateTime.of(2026, 6, 1, 11, 0);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(10L);
        request.setName("Audit Sprint 3");
        request.setDescription("Revue du code");
        request.setStartDate(start);
        request.setEndDate(end);
        request.setProjectGroupIds(Set.of(5L));

        Event saved = Event.builder()
                .id(1L)
                .name("Audit Sprint 3")
                .description("Revue du code")
                .categoryType(CategoryType.AUDIT)
                .category(eventCategory)
                .createdBy(teacher)
                .startDate(start)
                .endDate(end)
                .projectGroups(Set.of(projectGroup))
                .build();

        when(userService.findByEmail("teacher@teacher.fr")).thenReturn(teacher);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(eventCategory));
        when(studentGroupRepository.findAllById(Set.of(5L))).thenReturn(List.of(projectGroup));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventResponseDto result = eventService.create(request, "teacher@teacher.fr");

        assertNotNull(result);
        assertNotNull(result.getProjectGroups());
        assertEquals(1, result.getProjectGroups().size());
        assertEquals("Projet Alpha", result.getProjectGroups().get(0).getName());
        assertEquals("Projet Horizon", result.getProjectGroups().get(0).getCategoryName());
        verify(studentGroupRepository).findAllById(Set.of(5L));
    }

    @Test
    void create_should_not_assign_project_groups_when_no_projectGroupIds() {

        Role teacherRole = new Role("TEACHER");

        User teacher = new User();
        teacher.setId(1L);
        teacher.setName("Sophie ROUSSEAU");
        teacher.setEmail("teacher@teacher.fr");
        teacher.setRoles(Set.of(teacherRole));

        Category eventCategory = new Category();
        eventCategory.setId(10L);
        eventCategory.setName("UI/UX");
        eventCategory.setCategoryType(CategoryType.AUDIT);

        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 9, 0);
        LocalDateTime end   = LocalDateTime.of(2026, 6, 1, 11, 0);

        EventRequestDto request = new EventRequestDto();
        request.setCategoryId(10L);
        request.setName("Audit Sprint 5");
        request.setStartDate(start);
        request.setEndDate(end);
        // Pas de projectGroupIds

        Event saved = Event.builder()
                .id(7L)
                .name("Audit Sprint 5")
                .categoryType(CategoryType.AUDIT)
                .category(eventCategory)
                .createdBy(teacher)
                .startDate(start)
                .endDate(end)
                .build();

        when(userService.findByEmail("teacher@teacher.fr")).thenReturn(teacher);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(eventCategory));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventResponseDto result = eventService.create(request, "teacher@teacher.fr");

        assertNotNull(result);
        verify(studentGroupRepository, never()).findAllById(any());
    }

    @Test
    void listByType_should_include_parentGroupName_in_project_group_ref() {

        User teacher = new User();
        teacher.setId(1L);
        teacher.setName("Sophie ROUSSEAU");

        Category eventCat = new Category();
        eventCat.setId(10L);
        eventCat.setName("UI/UX");

        Category groupCat = new Category();
        groupCat.setId(20L);
        groupCat.setName("Projet Horizon");

        StudentGroup optionGroup = StudentGroup.builder()
                .id(100L)
                .name("LD — Promo 2026")
                .type(GroupType.OPTION)
                .category(groupCat)
                .createdBy(teacher)
                .members(new HashSet<>())
                .build();

        StudentGroup projectGroup = StudentGroup.builder()
                .id(5L)
                .name("Projet Alpha")
                .type(GroupType.PROJECT)
                .category(groupCat)
                .createdBy(teacher)
                .parentGroup(optionGroup)
                .members(new HashSet<>())
                .build();

        Event event = Event.builder()
                .id(1L)
                .name("Audit Sprint 1")
                .description("Revue sprint 1")
                .categoryType(CategoryType.AUDIT)
                .category(eventCat)
                .createdBy(teacher)
                .projectGroups(Set.of(projectGroup))
                .build();

        when(eventRepository.findByCategoryTypeWithGroups(CategoryType.AUDIT))
                .thenReturn(List.of(event));

        List<EventResponseDto> result = eventService.listByType(CategoryType.AUDIT);

        assertEquals(1, result.size());
        assertNotNull(result.get(0).getProjectGroups());
        assertEquals(1, result.get(0).getProjectGroups().size());

        EventResponseDto.ProjectGroupRefDto ref = result.get(0).getProjectGroups().get(0);
        assertEquals("Projet Alpha", ref.getName());
        assertEquals("Projet Horizon", ref.getCategoryName());
        assertEquals("LD — Promo 2026", ref.getParentGroupName());
    }

    // =============================================
    // deleteById()
    // =============================================

    @Test
    void deleteById_should_delete_event_when_found() {

        Event event = Event.builder()
                .id(1L)
                .name("Audit Sprint 1")
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        eventService.deleteById(1L);

        verify(eventRepository).findById(1L);
        verify(eventRepository).delete(event);
    }

    @Test
    void deleteById_should_throw_EventNotFoundException_when_not_found() {

        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class,
                () -> eventService.deleteById(99L));

        verify(eventRepository, never()).delete(any());
    }
}
