package fr.horizon.backend_api.service;

import fr.horizon.backend_api.controller.NotificationController;
import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.exception.EventConflictException;
import fr.horizon.backend_api.exception.EventNotFoundException;
import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.EventInvitationResponseDto;
import fr.horizon.backend_api.model.dto.EventRequestDto;
import fr.horizon.backend_api.model.dto.EventResponseDto;
import fr.horizon.backend_api.model.dto.FollowersInfoDto;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.model.enums.GroupType;
import fr.horizon.backend_api.model.enums.TeacherType;
import fr.horizon.backend_api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
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

    @Mock
    private UserRepository userRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private IssueFollowerRepository issueFollowerRepository;

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

        when(eventRepository.findByCategoryTypeOrderByCreatedAtDesc(CategoryType.AUDIT))
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

    // =============================================
    // Nouvelles méthodes de test pour EventService
    // =============================================

    @Test
    @DisplayName("createMeeting : Doit créer un meeting et l'ajouter au participant courant")
    void createMeeting_success() {
        User teacher = new User(); teacher.setId(1L); teacher.setRoles(Set.of(new Role("TEACHER")));
        Issue issue = new Issue(); issue.setId(100L); issue.setCategory(new Category());

        EventRequestDto dto = new EventRequestDto();
        dto.setName("Réunion");
        dto.setStartDate(LocalDateTime.now().plusDays(1));
        dto.setEndDate(LocalDateTime.now().plusDays(1).plusHours(1));

        when(userService.findByEmail(anyString())).thenReturn(teacher);
        when(issueRepository.findById(100L)).thenReturn(Optional.of(issue));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArguments()[0]);

        EventResponseDto result = eventService.createMeeting(dto, "test@test.fr", 100L);

        assertNotNull(result);
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    @DisplayName("inviteUserToMeeting : Doit ajouter un participant si aucun conflit")
    void inviteUserToMeeting_success() {
        User user = new User(); user.setId(2L);
        Category category = new Category();
        category.setName("Audit");

        Event event = Event.builder()
                .id(1L)
                .category(category)
                .createdBy(new User())
                .participants(new HashSet<>())
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArguments()[0]);

        eventService.inviteUserToMeeting(1L, 2L);

        assertEquals(1, event.getParticipants().size());
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("responseToMeetingInvitation : Doit valider l'événement si l'invité accepte")
    void responseToMeetingInvitation_accept() {
        User user = new User(); user.setId(2L);
        Category category = new Category();
        category.setName("Audit");

        Event event = Event.builder()
                .id(1L)
                .category(category)
                .createdBy(new User())
                .participants(new HashSet<>())
                .build();
        event.addParticipant(user, null);

        when(userService.findByEmail(anyString())).thenReturn(user);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArguments()[0]);

        eventService.responseToMeetingInvitation(1L, true, "test@test.fr", null);

        assertTrue(event.getParticipants().iterator().next().getHasAccepted());
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("getInvitationsToEvent : Doit retourner les invitations en attente")
    void getInvitationsToEvent_returnsList() {
        User user = new User(); user.setId(1L);
        Category category = new Category();
        category.setName("Audit");

        when(userService.findByEmail(anyString())).thenReturn(user);
        when(eventRepository.findPendingInvitationsByUserId(1L)).thenReturn(List.of(
                Event.builder()
                .name("test")
                .category(category)
                .createdBy(user).id(1L)
                .build()));


        List<EventInvitationResponseDto> result = eventService.getInvitationsToEvent("test@test.fr");

        assertEquals(1, result.size());
        verify(eventRepository).findPendingInvitationsByUserId(1L);
    }

    @Test
    @DisplayName("getEventInvitationStatut : Doit échouer si l'utilisateur n'est pas le créateur")
    void getEventInvitationStatut_denied() {
        User requester = new User(); requester.setId(99L);
        Category category = new Category();
        category.setName("Audit");

        Event event = Event.builder()
                .id(1L)
                .category(category)
                .createdBy(new User())
                .participants(new HashSet<>())
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userService.findByEmail(anyString())).thenReturn(requester);

        assertThrows(IllegalArgumentException.class, () -> eventService.getEventInvitationStatut(1L, "fake@mail.com"));
    }

    @Test
    @DisplayName("cancelEvent : Doit supprimer l'événement après avoir vidé les participants")
    void cancelEvent_success() {
        Category category = new Category();
        category.setName("Audit");

        Event event = Event.builder()
                .id(1L)
                .category(category)
                .createdBy(new User())
                .participants(new HashSet<>())
                .build();        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        eventService.cancelEvent(1L);

        verify(eventRepository).delete(event);
        verify(eventRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("updateMeetingDueToChange : Doit réinitialiser les invitations si changement d'horaire")
    void updateMeetingDueToChange_success() {
        Category category = new Category();
        category.setName("Audit");

        Event event = Event.builder()
                .id(1L)
                .category(category)
                .createdBy(new User())
                .participants(new HashSet<>())
                .build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.updateMeetingDueToChange(1L, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), true);

        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("deleteParticipantFromMeeting : Doit retirer un participant")
    void deleteParticipantFromMeeting_success() {
        User user = new User(); user.setId(2L);
        Category category = new Category();
        category.setName("Audit");

        Event event = Event.builder()
                .id(1L)
                .category(category)
                .createdBy(new User())
                .participants(new HashSet<>())
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(eventRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.deleteParticipantFromMeeting(1L, 2L);

        verify(eventRepository).save(any(Event.class));
    }

    // =============================================
// inviteUserToMeeting() — conflits
// =============================================

    @Test
    @DisplayName("inviteUserToMeeting : doit lever une exception si conflit agenda")
    void inviteUserToMeeting_should_throw_when_conflict() {

        User user = new User();
        user.setId(2L);
        user.setName("Jean");

        Event event = Event.builder()
                .id(1L)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(1))
                .participants(new HashSet<>())
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(eventRepository.countConflictingEvents(any(), any(), any(), any()))
                .thenReturn(1L);

        assertThrows(EventConflictException.class,
                () -> eventService.inviteUserToMeeting(1L, 2L));

        verify(eventRepository, never()).save(any());
    }

// =============================================
// responseToMeetingInvitation() — refus
// =============================================

    @Test
    @DisplayName("responseToMeetingInvitation : doit enregistrer la raison du refus")
    void responseToMeetingInvitation_refuse() {

        User user = new User();
        user.setId(2L);

        User manager = new User();
        manager.setId(99L);

        Category category = new Category();
        category.setName("Audit");

        Issue issue = new Issue();
        issue.setManager(manager);

        Event event = Event.builder()
                .id(1L)
                .category(category)
                .issue(issue) // ✅ FIX
                .createdBy(new User())
                .participants(new HashSet<>())
                .build();

        event.addParticipant(user, null);

        when(userService.findByEmail(anyString())).thenReturn(user);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.responseToMeetingInvitation(
                1L,
                false,
                "user@test.fr",
                "Je ne suis pas disponible"
        );

        EventParticipant participant = event.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(2L))
                .findFirst()
                .orElseThrow();

        assertFalse(participant.getHasAccepted());
        assertEquals("Je ne suis pas disponible", participant.getReason());
    }

// =============================================
// updateMeetingDueToChange() — addAll false
// =============================================

    @Test
    @DisplayName("updateMeetingDueToChange : doit retirer participants non acceptés")
    void updateMeetingDueToChange_remove_unaccepted_participants() {

        User creator = new User();
        creator.setId(1L);

        User invited = new User();
        invited.setId(2L);

        Category category = new Category();
        category.setName("Audit");

        Event event = Event.builder()
                .id(1L)
                .category(category) // <-- IMPORTANT
                .createdBy(creator)
                .participants(new HashSet<>())
                .build();

        event.addParticipant(creator, true);
        event.addParticipant(invited, false);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.updateMeetingDueToChange(
                1L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                false
        );

        assertEquals(1, event.getParticipants().size());
    }

// =============================================
// askForMeeting()
// =============================================

    @Test
    @DisplayName("askForMeeting : doit créer une demande non validée")
    void askForMeeting_success() {

        User student = new User();
        student.setId(1L);

        User manager = new User();
        manager.setId(2L);

        Category category = new Category();
        category.setName("Backend");

        Issue issue = new Issue();
        issue.setId(100L);
        issue.setCategory(category);
        issue.setManager(manager);

        EventRequestDto dto = new EventRequestDto();
        dto.setName("Demande réunion");
        dto.setStartDate(LocalDateTime.now().plusDays(1));
        dto.setEndDate(LocalDateTime.now().plusDays(1).plusHours(1));

        when(userService.findByEmail(anyString())).thenReturn(student);
        when(issueRepository.findById(100L)).thenReturn(Optional.of(issue));
        when(eventRepository.save(any())).thenAnswer(i -> {
            Event e = i.getArgument(0);
            e.setId(1L);
            return e;
        });

        when(eventRepository.findById(1L))
                .thenAnswer(i -> Optional.of(
                        (Event) mock(Event.class, RETURNS_DEEP_STUBS)
                ));

        // mock simplifié pour éviter conflit
        Event savedEvent = Event.builder()
                .id(1L)
                .category(category)
                .createdBy(student)
                .participants(new HashSet<>())
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(savedEvent));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        EventResponseDto result =
                eventService.askForMeeting(dto, "student@test.fr", 100L);

        assertNotNull(result);

        verify(eventRepository, atLeastOnce()).save(any());
    }

// =============================================
// issueGotMeeting()
// =============================================

    @Test
    @DisplayName("issueGotMeeting : retourne true si réunion future existe")
    void issueGotMeeting_true() {

        when(eventRepository.existsByIssueIdAndStartDateAfterAndValidatedTrue(
                eq(100L),
                any(LocalDateTime.class)
        )).thenReturn(true);

        boolean result = eventService.issueGotMeeting(100L);

        assertTrue(result);
    }

// =============================================
// getMyDemandsStatus()
// =============================================

    @Test
    @DisplayName("getMyDemandsStatus : retourne les demandes")
    void getMyDemandsStatus_success() {

        User user = new User();
        user.setId(1L);

        Issue issue = new Issue();
        issue.setId(100L);

        Event event = Event.builder()
                .id(1L)
                .issue(issue)
                .validated(true)
                .build();

        when(userService.findByEmail(anyString())).thenReturn(user);

        when(eventRepository.findByCreatedByIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(event));

        var result = eventService.getMyDemandsStatus("user@test.fr");

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getIssueId());
        assertTrue(result.get(0).getStatus());
    }

// =============================================
// getMyCreatedEvents()
// =============================================

    @Test
    @DisplayName("getMyCreatedEvents : retourne les events créés")
    void getMyCreatedEvents_success() {

        User user = new User();
        user.setId(1L);
        user.setName("Teacher");

        Category category = new Category();
        category.setName("Audit");

        Event event = Event.builder()
                .id(1L)
                .name("Meeting")
                .category(category)
                .createdBy(user)
                .participants(new HashSet<>())
                .build();

        when(userService.findByEmail(anyString())).thenReturn(user);

        when(eventRepository.findEventsManagedOrCreatedByUser(1L))
                .thenReturn(List.of(event));

        var result = eventService.getMyCreatedEvents("teacher@test.fr");

        assertEquals(1, result.size());
        assertEquals("Meeting", result.get(0).getName());
    }

// =============================================
// getPossibleUserInterested()
// =============================================

    @Test
    @DisplayName("getPossibleUserInterested : retourne followers non invités")
    void getPossibleUserInterested_success() {

        User invited = new User();
        invited.setId(1L);

        User follower = new User();
        follower.setId(2L);
        follower.setName("Follower");

        Issue issue = new Issue();
        issue.setId(100L);

        Event event = Event.builder()
                .id(1L)
                .issue(issue)
                .participants(new HashSet<>())
                .build();

        event.addParticipant(invited, true);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        when(issueFollowerRepository.findUsersByIssueId(100L))
                .thenReturn(List.of(invited, follower));

        var result = eventService.getPossibleUserInterested(1L);

        assertEquals(1, result.size());
        assertEquals("Follower", result.get(0).getName());
    }

    @Test
    @DisplayName("responseToMeetingInvitation : doit valider l'événement si le manager répond")
    void responseToMeetingInvitation_manager_accepts_event() {

        User manager = new User();
        manager.setId(99L);

        User user = new User();
        user.setId(2L);

        Issue issue = new Issue();
        issue.setManager(manager);
        issue.setCategory(new Category());

        Category category = new Category();
        category.setName("Audit");

        Event event = Event.builder()
                .id(1L)
                .issue(issue)
                .category(category)
                .createdBy(manager)
                .participants(new HashSet<>())
                .build();

        event.addParticipant(manager, null);

        when(userService.findByEmail(anyString())).thenReturn(manager);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.responseToMeetingInvitation(
                1L,
                true,
                "manager@test.fr",
                null
        );

        assertTrue(event.getValidated());
    }

    @Test
    @DisplayName("responseToMeetingInvitation : doit lever exception si utilisateur non invité")
    void responseToMeetingInvitation_user_not_invited() {

        User user = new User();
        user.setId(2L);

        Issue issue = new Issue();
        issue.setManager(new User());

        Event event = Event.builder()
                .id(1L)
                .issue(issue)
                .participants(new HashSet<>())
                .build();

        when(userService.findByEmail(anyString())).thenReturn(user);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(EntityNotFoundException.class,
                () -> eventService.responseToMeetingInvitation(
                        1L, true, "user@test.fr", null
                ));
    }

    @Test
    @DisplayName("getPossibleUserInterested : retourne liste vide si pas d'issue")
    void getPossibleUserInterested_no_issue() {

        Event event = Event.builder()
                .id(1L)
                .issue(null)
                .participants(new HashSet<>())
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        List<FollowersInfoDto> result =
                eventService.getPossibleUserInterested(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("cancelEvent : doit lever exception si event introuvable")
    void cancelEvent_not_found() {

        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> eventService.cancelEvent(99L));

        verify(eventRepository, never()).delete(any());
    }

}
