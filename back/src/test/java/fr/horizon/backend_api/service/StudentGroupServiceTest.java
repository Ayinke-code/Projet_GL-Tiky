package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.FileTooLargeException;
import fr.horizon.backend_api.exception.InvalidGroupSizeException;
import fr.horizon.backend_api.exception.StudentAlreadyInGroupException;
import fr.horizon.backend_api.exception.StudentGroupNotFoundException;
import fr.horizon.backend_api.exception.UnsupportedFileFormatException;
import fr.horizon.backend_api.exception.UserNotFoundException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.StudentGroup;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.StudentGroupCreateDto;
import fr.horizon.backend_api.model.dto.StudentGroupDto;
import fr.horizon.backend_api.model.dto.StudentImportResultDto;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.model.enums.GroupType;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.EventRepository;
import fr.horizon.backend_api.repository.RoleRepository;
import fr.horizon.backend_api.repository.StudentGroupRepository;
import fr.horizon.backend_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentGroupServiceTest {

    @Mock
    private StudentGroupRepository studentGroupRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StudentGroupService studentGroupService;

    private User creator;
    private Category category;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(99L);
        creator.setName("Sophie ROUSSEAU");
        creator.setEmail("teacher@teacher.fr");
        creator.setRoles(Set.of(new Role("TEACHER")));

        category = new Category();
        category.setId(10L);
        category.setName("Projet Horizon");
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    /**
     * Crée n utilisateurs et les enregistre dans les mocks.
     * Chaque utilisateur a un id de 1 à n.
     */
    private Set<Long> mockNStudents(int n, GroupType type) {
        Set<Long> ids = new HashSet<>();
        for (long i = 1; i <= n; i++) {
            User student = new User();
            student.setId(i);
            student.setName("Étudiant " + i);
            student.setRoles(Set.of(new Role("STUDENT")));
            when(userRepository.findById(i)).thenReturn(Optional.of(student));
            when(studentGroupRepository.findByMemberIdAndType(i, type)).thenReturn(List.of());
            ids.add(i);
        }
        return ids;
    }

    private StudentGroupCreateDto buildDto(String name, GroupType type, Long categoryId, Set<Long> memberIds) {
        StudentGroupCreateDto dto = new StudentGroupCreateDto();
        dto.setName(name);
        dto.setType(type);
        dto.setCategoryId(categoryId);
        dto.setMemberIds(memberIds);
        return dto;
    }

    // -------------------------------------------------------
    // create()
    // -------------------------------------------------------

    @Test
    void create_should_save_option_group_with_categoryId() {
        Set<Long> memberIds = mockNStudents(24, GroupType.OPTION);

        StudentGroupCreateDto dto = buildDto("Groupe LD", GroupType.OPTION, 10L, memberIds);

        StudentGroup saved = StudentGroup.builder()
                .id(1L)
                .name("Groupe LD")
                .type(GroupType.OPTION)
                .category(category)
                .createdBy(creator)
                .members(new HashSet<>())
                .build();

        when(userRepository.findByEmail("teacher@teacher.fr")).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(saved);

        StudentGroupDto result = studentGroupService.create(dto, "teacher@teacher.fr");

        assertNotNull(result);
        assertEquals("Groupe LD", result.getName());
        assertEquals(GroupType.OPTION, result.getType());
        verify(studentGroupRepository).save(any(StudentGroup.class));
    }

    @Test
    void create_should_save_project_group_with_categoryId() {
        Set<Long> memberIds = mockNStudents(2, GroupType.PROJECT);

        StudentGroupCreateDto dto = buildDto("Groupe Horizon A", GroupType.PROJECT, 10L, memberIds);

        StudentGroup saved = StudentGroup.builder()
                .id(2L)
                .name("Groupe Horizon A")
                .type(GroupType.PROJECT)
                .category(category)
                .createdBy(creator)
                .members(new HashSet<>())
                .build();

        when(userRepository.findByEmail("teacher@teacher.fr")).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(saved);

        StudentGroupDto result = studentGroupService.create(dto, "teacher@teacher.fr");

        assertNotNull(result);
        assertEquals("Groupe Horizon A", result.getName());
        assertEquals(GroupType.PROJECT, result.getType());
    }

    @Test
    void create_should_reuse_existing_category_when_customCategoryName_matches() {
        Set<Long> memberIds = mockNStudents(24, GroupType.OPTION);

        StudentGroupCreateDto dto = buildDto("Groupe LD", GroupType.OPTION, null, memberIds);
        dto.setCustomCategoryName("Logiciel & DevOps");

        Category existing = new Category();
        existing.setId(55L);
        existing.setName("Logiciel & DevOps");

        when(userRepository.findByEmail("teacher@teacher.fr")).thenReturn(Optional.of(creator));
        when(categoryRepository.findByNameIgnoreCase("Logiciel & DevOps")).thenReturn(Optional.of(existing));

        StudentGroup saved = StudentGroup.builder()
                .id(3L)
                .name("Groupe LD")
                .type(GroupType.OPTION)
                .category(existing)
                .createdBy(creator)
                .members(new HashSet<>())
                .build();

        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(saved);

        StudentGroupDto result = studentGroupService.create(dto, "teacher@teacher.fr");

        assertEquals("Logiciel & DevOps", result.getCategoryName());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void create_should_create_new_category_when_customCategoryName_is_unknown() {
        Set<Long> memberIds = mockNStudents(24, GroupType.OPTION);

        StudentGroupCreateDto dto = buildDto("Groupe Nouveau", GroupType.OPTION, null, memberIds);
        dto.setCustomCategoryName("Nouvelle filière");

        Category newCat = Category.builder()
                .id(66L)
                .name("Nouvelle filière")
                .subject("Nouvelle filière")
                .categoryType(CategoryType.OPTION)
                .status(CategoryStatus.ACTIVE)
                .owner(creator)
                .build();

        when(userRepository.findByEmail("teacher@teacher.fr")).thenReturn(Optional.of(creator));
        when(categoryRepository.findByNameIgnoreCase("Nouvelle filière")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(newCat);

        StudentGroup saved = StudentGroup.builder()
                .id(4L)
                .name("Groupe Nouveau")
                .type(GroupType.OPTION)
                .category(newCat)
                .createdBy(creator)
                .members(new HashSet<>())
                .build();

        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(saved);

        StudentGroupDto result = studentGroupService.create(dto, "teacher@teacher.fr");

        assertEquals("Nouvelle filière", result.getCategoryName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_should_link_parent_group_when_parentGroupId_provided_for_project() {
        Set<Long> memberIds = mockNStudents(2, GroupType.PROJECT);

        StudentGroupCreateDto dto = buildDto("Projet Alpha", GroupType.PROJECT, 10L, memberIds);
        dto.setParentGroupId(100L);

        StudentGroup optionGroup = StudentGroup.builder()
                .id(100L)
                .name("LD — Promo 2026")
                .type(GroupType.OPTION)
                .category(category)
                .createdBy(creator)
                .members(new HashSet<>())
                .build();

        StudentGroup saved = StudentGroup.builder()
                .id(5L)
                .name("Projet Alpha")
                .type(GroupType.PROJECT)
                .category(category)
                .createdBy(creator)
                .parentGroup(optionGroup)
                .members(new HashSet<>())
                .build();

        when(userRepository.findByEmail("teacher@teacher.fr")).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(studentGroupRepository.findById(100L)).thenReturn(Optional.of(optionGroup));
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(saved);

        StudentGroupDto result = studentGroupService.create(dto, "teacher@teacher.fr");

        assertEquals("LD — Promo 2026", result.getParentGroupName());
        assertEquals(100L, result.getParentGroupId());
        verify(studentGroupRepository).findById(100L);
    }

    @Test
    void create_should_throw_InvalidGroupSizeException_when_option_group_has_fewer_than_24_members() {
        Set<Long> memberIds = mockNStudents(10, GroupType.OPTION);

        StudentGroupCreateDto dto = buildDto("Groupe Trop Petit", GroupType.OPTION, 10L, memberIds);

        when(userRepository.findByEmail("teacher@teacher.fr")).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        assertThrows(InvalidGroupSizeException.class,
                () -> studentGroupService.create(dto, "teacher@teacher.fr"));

        verify(studentGroupRepository, never()).save(any());
    }

    @Test
    void create_should_throw_InvalidGroupSizeException_when_project_group_has_only_one_member() {
        Set<Long> memberIds = mockNStudents(1, GroupType.PROJECT);

        StudentGroupCreateDto dto = buildDto("Projet Solo", GroupType.PROJECT, 10L, memberIds);

        when(userRepository.findByEmail("teacher@teacher.fr")).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        assertThrows(InvalidGroupSizeException.class,
                () -> studentGroupService.create(dto, "teacher@teacher.fr"));

        verify(studentGroupRepository, never()).save(any());
    }

    @Test
    void create_should_throw_StudentAlreadyInGroupException_when_student_already_in_a_group_of_same_type() {
        long studentId = 1L;
        User student = new User();
        student.setId(studentId);
        student.setName("Alice Dupont");
        student.setRoles(Set.of(new Role("STUDENT")));

        // Un seul membre pour déclencher l'exception avant la validation taille
        Set<Long> memberIds = LongStream.rangeClosed(1, 24)
                .boxed()
                .collect(Collectors.toSet());

        when(userRepository.findByEmail("teacher@teacher.fr")).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        // Alice est déjà dans un groupe OPTION
        StudentGroup existingGroup = StudentGroup.builder().id(99L).name("Groupe Existant").type(GroupType.OPTION).build();
        when(studentGroupRepository.findByMemberIdAndType(1L, GroupType.OPTION))
                .thenReturn(List.of(existingGroup));

        StudentGroupCreateDto dto = buildDto("Nouveau Groupe", GroupType.OPTION, 10L, memberIds);

        assertThrows(StudentAlreadyInGroupException.class,
                () -> studentGroupService.create(dto, "teacher@teacher.fr"));

        verify(studentGroupRepository, never()).save(any());
    }

    @Test
    void create_should_throw_UserNotFoundException_when_member_does_not_exist() {
        // memberIds contient un id inconnu
        Set<Long> memberIds = Set.of(999L);

        StudentGroupCreateDto dto = buildDto("Groupe Fantôme", GroupType.PROJECT, 10L, memberIds);

        when(userRepository.findByEmail("teacher@teacher.fr")).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> studentGroupService.create(dto, "teacher@teacher.fr"));

        verify(studentGroupRepository, never()).save(any());
    }

    // -------------------------------------------------------
    // findAll()
    // -------------------------------------------------------

    @Test
    void findAll_should_return_all_groups_mapped_to_dto() {
        StudentGroup g1 = StudentGroup.builder()
                .id(1L)
                .name("Groupe LD")
                .type(GroupType.OPTION)
                .category(category)
                .createdBy(creator)
                .members(new HashSet<>())
                .build();

        StudentGroup g2 = StudentGroup.builder()
                .id(2L)
                .name("Projet Alpha")
                .type(GroupType.PROJECT)
                .category(category)
                .createdBy(creator)
                .members(new HashSet<>())
                .build();

        when(studentGroupRepository.findAllByOrderByTypeAscNameAsc()).thenReturn(List.of(g1, g2));

        List<StudentGroupDto> result = studentGroupService.findAll();

        assertEquals(2, result.size());
        assertEquals("Groupe LD", result.get(0).getName());
        assertEquals("Projet Alpha", result.get(1).getName());
    }

    @Test
    void findAll_should_return_empty_list_when_no_groups_exist() {
        when(studentGroupRepository.findAllByOrderByTypeAscNameAsc()).thenReturn(List.of());

        List<StudentGroupDto> result = studentGroupService.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------
    // findByType()
    // -------------------------------------------------------

    @Test
    void findByType_should_return_only_option_groups() {
        StudentGroup optionGroup = StudentGroup.builder()
                .id(1L)
                .name("Groupe LD")
                .type(GroupType.OPTION)
                .category(category)
                .createdBy(creator)
                .members(new HashSet<>())
                .build();

        when(studentGroupRepository.findByTypeOrderByNameAsc(GroupType.OPTION))
                .thenReturn(List.of(optionGroup));

        List<StudentGroupDto> result = studentGroupService.findByType(GroupType.OPTION);

        assertEquals(1, result.size());
        assertEquals(GroupType.OPTION, result.get(0).getType());
        verify(studentGroupRepository).findByTypeOrderByNameAsc(GroupType.OPTION);
    }

    @Test
    void findByType_should_return_empty_list_when_no_groups_of_type_exist() {
        when(studentGroupRepository.findByTypeOrderByNameAsc(GroupType.PROJECT))
                .thenReturn(List.of());

        List<StudentGroupDto> result = studentGroupService.findByType(GroupType.PROJECT);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findByType_should_include_parentGroupName_for_project_groups() {
        StudentGroup parent = StudentGroup.builder()
                .id(10L)
                .name("LD — Promo 2026")
                .type(GroupType.OPTION)
                .category(category)
                .createdBy(creator)
                .members(new HashSet<>())
                .build();

        StudentGroup projectGroup = StudentGroup.builder()
                .id(2L)
                .name("Projet Horizon A")
                .type(GroupType.PROJECT)
                .category(category)
                .createdBy(creator)
                .parentGroup(parent)
                .members(new HashSet<>())
                .build();

        when(studentGroupRepository.findByTypeOrderByNameAsc(GroupType.PROJECT))
                .thenReturn(List.of(projectGroup));

        List<StudentGroupDto> result = studentGroupService.findByType(GroupType.PROJECT);

        assertEquals(1, result.size());
        assertEquals("LD — Promo 2026", result.get(0).getParentGroupName());
        assertEquals(10L, result.get(0).getParentGroupId());
    }

    // -------------------------------------------------------
    // deleteById()
    // -------------------------------------------------------

    @Test
    void deleteById_should_delete_group_when_found() {

        StudentGroup group = StudentGroup.builder()
                .id(1L)
                .name("Groupe LD")
                .type(GroupType.OPTION)
                .category(category)
                .createdBy(creator)
                .members(new HashSet<>())
                .build();

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(studentGroupRepository.findByParentGroup_Id(1L)).thenReturn(List.of());

        studentGroupService.deleteById(1L);

        verify(studentGroupRepository).findById(1L);
        verify(eventRepository).removeGroupFromAllEvents(1L);
        verify(studentGroupRepository).delete(group);
    }

    @Test
    void deleteById_should_throw_StudentGroupNotFoundException_when_not_found() {

        when(studentGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(fr.horizon.backend_api.exception.StudentGroupNotFoundException.class,
                () -> studentGroupService.deleteById(99L));

        verify(studentGroupRepository, never()).delete(any());
    }

    @Test
    void deleteById_cascade_promotion_should_cascade_to_options_and_projects() {

        StudentGroup projectGroup = StudentGroup.builder()
                .id(3L).name("Projet Alpha").type(GroupType.PROJECT)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        StudentGroup optionGroup = StudentGroup.builder()
                .id(2L).name("Option LD").type(GroupType.OPTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        StudentGroup promoGroup = StudentGroup.builder()
                .id(1L).name("Promo 2026").type(GroupType.PROMOTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(promoGroup));
        when(studentGroupRepository.findByPromotion_Id(1L)).thenReturn(List.of(optionGroup));
        when(studentGroupRepository.findByParentGroup_Id(2L)).thenReturn(List.of(projectGroup));

        studentGroupService.deleteById(1L);

        verify(eventRepository).removeGroupFromAllEvents(3L);
        verify(studentGroupRepository).delete(projectGroup);
        verify(eventRepository).removeGroupFromAllEvents(2L);
        verify(studentGroupRepository).delete(optionGroup);
        verify(eventRepository).removeGroupFromAllEvents(1L);
        verify(studentGroupRepository).delete(promoGroup);
    }

    @Test
    void findMyGroups_should_return_groups_for_student() {

        User student = new User();
        student.setId(42L);
        student.setEmail("student@student.fr");
        student.setRoles(Set.of(new Role("STUDENT")));

        StudentGroup optionGroup = StudentGroup.builder()
                .id(7L).name("Option LD").type(GroupType.OPTION)
                .category(category).createdBy(creator).members(new HashSet<>())
                .teachers(new HashSet<>()).build();

        when(userRepository.findByEmail("student@student.fr")).thenReturn(Optional.of(student));
        when(studentGroupRepository.findByMemberIdAndType(42L, GroupType.OPTION))
                .thenReturn(List.of(optionGroup));

        List<StudentGroupDto> result = studentGroupService.findMyGroups("student@student.fr", GroupType.OPTION);

        assertEquals(1, result.size());
        assertEquals("Option LD", result.get(0).getName());
        verify(studentGroupRepository).findByMemberIdAndType(42L, GroupType.OPTION);
    }

    @Test
    void findMyGroups_should_throw_UserNotFoundException_when_email_not_found() {

        when(userRepository.findByEmail("unknown@student.fr")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> studentGroupService.findMyGroups("unknown@student.fr", GroupType.OPTION));
    }

    // -------------------------------------------------------
    // Helper pour update (sans mock findByMemberIdAndType)
    // -------------------------------------------------------

    private Set<Long> mockNStudentsForUpdate(int n) {
        Set<Long> ids = new HashSet<>();
        for (long i = 1; i <= n; i++) {
            User student = new User();
            student.setId(i);
            student.setName("Étudiant " + i);
            student.setRoles(Set.of(new Role("STUDENT")));
            when(userRepository.findById(i)).thenReturn(Optional.of(student));
            ids.add(i);
        }
        return ids;
    }

    // -------------------------------------------------------
    // create() — PROMOTION
    // -------------------------------------------------------

    @Test
    void create_promotion_should_reuse_existing_promotion_category() {
        StudentGroupCreateDto dto = buildDto("Promo 2026", GroupType.PROMOTION, null, null);

        Category promoCat = new Category();
        promoCat.setId(5L);
        promoCat.setName("Promotion");

        StudentGroup saved = StudentGroup.builder()
                .id(10L).name("Promo 2026").type(GroupType.PROMOTION)
                .category(promoCat).createdBy(creator).members(new HashSet<>()).build();

        when(userRepository.findByEmail("teacher@teacher.fr")).thenReturn(Optional.of(creator));
        when(categoryRepository.findByNameIgnoreCase("Promotion")).thenReturn(Optional.of(promoCat));
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(saved);

        StudentGroupDto result = studentGroupService.create(dto, "teacher@teacher.fr");

        assertEquals("Promo 2026", result.getName());
        assertEquals("Promotion", result.getCategoryName());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void create_promotion_should_create_default_category_when_none_exists() {
        StudentGroupCreateDto dto = buildDto("Promo 2027", GroupType.PROMOTION, null, null);

        Category newCat = Category.builder()
                .id(6L).name("Promotion").subject("Promotion")
                .categoryType(CategoryType.PROMOTION).status(CategoryStatus.ACTIVE).owner(creator).build();

        StudentGroup saved = StudentGroup.builder()
                .id(11L).name("Promo 2027").type(GroupType.PROMOTION)
                .category(newCat).createdBy(creator).members(new HashSet<>()).build();

        when(userRepository.findByEmail("teacher@teacher.fr")).thenReturn(Optional.of(creator));
        when(categoryRepository.findByNameIgnoreCase("Promotion")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(newCat);
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(saved);

        StudentGroupDto result = studentGroupService.create(dto, "teacher@teacher.fr");

        assertEquals("Promo 2027", result.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_option_should_link_promotion_when_promotionId_provided() {
        Set<Long> memberIds = mockNStudents(24, GroupType.OPTION);
        StudentGroupCreateDto dto = buildDto("LD-Promo2026", GroupType.OPTION, 10L, memberIds);
        dto.setPromotionId(50L);

        StudentGroup promo = StudentGroup.builder()
                .id(50L).name("Promo 2026").type(GroupType.PROMOTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        StudentGroup saved = StudentGroup.builder()
                .id(20L).name("LD-Promo2026").type(GroupType.OPTION)
                .category(category).createdBy(creator).promotion(promo).members(new HashSet<>()).build();

        when(userRepository.findByEmail("teacher@teacher.fr")).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(studentGroupRepository.findById(50L)).thenReturn(Optional.of(promo));
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(saved);

        StudentGroupDto result = studentGroupService.create(dto, "teacher@teacher.fr");

        assertEquals(50L, result.getPromotionId());
        assertEquals("Promo 2026", result.getPromotionName());
    }

    @Test
    void create_should_throw_when_name_already_exists_for_same_type() {
        StudentGroupCreateDto dto = buildDto("Groupe Existant", GroupType.OPTION, 10L, new HashSet<>());
        when(studentGroupRepository.existsByNameIgnoreCaseAndType("Groupe Existant", GroupType.OPTION))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> studentGroupService.create(dto, "teacher@teacher.fr"));
        verify(studentGroupRepository, never()).save(any());
    }

    // -------------------------------------------------------
    // findById()
    // -------------------------------------------------------

    @Test
    void findById_should_return_dto_when_group_exists() {
        StudentGroup group = StudentGroup.builder()
                .id(1L).name("Groupe LD").type(GroupType.OPTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        StudentGroupDto result = studentGroupService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Groupe LD", result.getName());
    }

    @Test
    void findById_should_throw_StudentGroupNotFoundException_when_not_found() {
        when(studentGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(StudentGroupNotFoundException.class,
                () -> studentGroupService.findById(99L));
    }

    // -------------------------------------------------------
    // update()
    // -------------------------------------------------------

    @Test
    void update_should_update_name_and_year() {
        StudentGroup group = StudentGroup.builder()
                .id(1L).name("Ancien Nom").type(GroupType.OPTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        StudentGroupCreateDto dto = new StudentGroupCreateDto();
        dto.setName("Nouveau Nom");
        dto.setYear(2027);

        StudentGroup saved = StudentGroup.builder()
                .id(1L).name("Nouveau Nom").type(GroupType.OPTION)
                .category(category).createdBy(creator).members(new HashSet<>()).year(2027).build();

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(studentGroupRepository.existsByNameIgnoreCaseAndTypeAndIdNot("Nouveau Nom", GroupType.OPTION, 1L))
                .thenReturn(false);
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(saved);

        StudentGroupDto result = studentGroupService.update(1L, dto, "teacher@teacher.fr");

        assertEquals("Nouveau Nom", result.getName());
        assertEquals(2027, result.getYear());
    }

    @Test
    void update_should_throw_StudentGroupNotFoundException_when_group_not_found() {
        when(studentGroupRepository.findById(999L)).thenReturn(Optional.empty());

        StudentGroupCreateDto dto = new StudentGroupCreateDto();
        dto.setName("Nom Quelconque");

        assertThrows(StudentGroupNotFoundException.class,
                () -> studentGroupService.update(999L, dto, "teacher@teacher.fr"));
    }

    @Test
    void update_should_throw_when_name_already_used_by_another_group() {
        StudentGroup group = StudentGroup.builder()
                .id(1L).name("Ancien").type(GroupType.OPTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        StudentGroupCreateDto dto = new StudentGroupCreateDto();
        dto.setName("Nom Pris");

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(studentGroupRepository.existsByNameIgnoreCaseAndTypeAndIdNot("Nom Pris", GroupType.OPTION, 1L))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> studentGroupService.update(1L, dto, "teacher@teacher.fr"));
    }

    @Test
    void update_should_update_category_by_categoryId() {
        StudentGroup group = StudentGroup.builder()
                .id(1L).name("Groupe LD").type(GroupType.OPTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        Category newCat = new Category();
        newCat.setId(20L);
        newCat.setName("Nouvelle Cat");

        StudentGroupCreateDto dto = new StudentGroupCreateDto();
        dto.setName("Groupe LD");
        dto.setCategoryId(20L);

        StudentGroup saved = StudentGroup.builder()
                .id(1L).name("Groupe LD").type(GroupType.OPTION)
                .category(newCat).createdBy(creator).members(new HashSet<>()).build();

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(studentGroupRepository.existsByNameIgnoreCaseAndTypeAndIdNot("Groupe LD", GroupType.OPTION, 1L))
                .thenReturn(false);
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(newCat));
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(saved);

        StudentGroupDto result = studentGroupService.update(1L, dto, "teacher@teacher.fr");

        assertEquals("Nouvelle Cat", result.getCategoryName());
        verify(categoryRepository).findById(20L);
    }

    @Test
    void update_should_update_category_by_customName_creating_new() {
        StudentGroup group = StudentGroup.builder()
                .id(1L).name("Groupe LD").type(GroupType.OPTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        Category newCat = Category.builder()
                .id(25L).name("Nouvelle Filière").subject("Nouvelle Filière")
                .categoryType(CategoryType.OPTION).status(CategoryStatus.ACTIVE).owner(creator).build();

        StudentGroupCreateDto dto = new StudentGroupCreateDto();
        dto.setName("Groupe LD");
        dto.setCustomCategoryName("Nouvelle Filière");

        StudentGroup saved = StudentGroup.builder()
                .id(1L).name("Groupe LD").type(GroupType.OPTION)
                .category(newCat).createdBy(creator).members(new HashSet<>()).build();

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(studentGroupRepository.existsByNameIgnoreCaseAndTypeAndIdNot("Groupe LD", GroupType.OPTION, 1L))
                .thenReturn(false);
        when(userRepository.findByEmail("teacher@teacher.fr")).thenReturn(Optional.of(creator));
        when(categoryRepository.findByNameIgnoreCase("Nouvelle Filière")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(newCat);
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(saved);

        StudentGroupDto result = studentGroupService.update(1L, dto, "teacher@teacher.fr");

        assertEquals("Nouvelle Filière", result.getCategoryName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void update_should_replace_members_for_option_group() {
        Set<Long> memberIds = mockNStudentsForUpdate(24);

        StudentGroup group = StudentGroup.builder()
                .id(1L).name("Groupe LD").type(GroupType.OPTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        StudentGroupCreateDto dto = new StudentGroupCreateDto();
        dto.setName("Groupe LD");
        dto.setMemberIds(memberIds);

        StudentGroup saved = StudentGroup.builder()
                .id(1L).name("Groupe LD").type(GroupType.OPTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(studentGroupRepository.existsByNameIgnoreCaseAndTypeAndIdNot("Groupe LD", GroupType.OPTION, 1L))
                .thenReturn(false);
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(saved);

        assertDoesNotThrow(() -> studentGroupService.update(1L, dto, "teacher@teacher.fr"));
        verify(studentGroupRepository).save(any(StudentGroup.class));
    }

    @Test
    void update_should_throw_InvalidGroupSizeException_when_option_members_below_24() {
        Set<Long> tooFew = mockNStudentsForUpdate(5);

        StudentGroup group = StudentGroup.builder()
                .id(1L).name("Groupe LD").type(GroupType.OPTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        StudentGroupCreateDto dto = new StudentGroupCreateDto();
        dto.setName("Groupe LD");
        dto.setMemberIds(tooFew);

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(studentGroupRepository.existsByNameIgnoreCaseAndTypeAndIdNot("Groupe LD", GroupType.OPTION, 1L))
                .thenReturn(false);

        assertThrows(InvalidGroupSizeException.class,
                () -> studentGroupService.update(1L, dto, "teacher@teacher.fr"));
    }

    @Test
    void update_should_set_parent_group_for_project_type() {
        StudentGroup group = StudentGroup.builder()
                .id(1L).name("Projet Alpha").type(GroupType.PROJECT)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        StudentGroup parentOption = StudentGroup.builder()
                .id(100L).name("LD-Promo2026").type(GroupType.OPTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        StudentGroupCreateDto dto = new StudentGroupCreateDto();
        dto.setName("Projet Alpha");
        dto.setParentGroupId(100L);

        StudentGroup saved = StudentGroup.builder()
                .id(1L).name("Projet Alpha").type(GroupType.PROJECT)
                .category(category).createdBy(creator)
                .parentGroup(parentOption).members(new HashSet<>()).build();

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(studentGroupRepository.existsByNameIgnoreCaseAndTypeAndIdNot("Projet Alpha", GroupType.PROJECT, 1L))
                .thenReturn(false);
        when(studentGroupRepository.findById(100L)).thenReturn(Optional.of(parentOption));
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(saved);

        StudentGroupDto result = studentGroupService.update(1L, dto, "teacher@teacher.fr");

        assertEquals("LD-Promo2026", result.getParentGroupName());
        assertEquals(100L, result.getParentGroupId());
    }

    @Test
    void update_should_link_promotion_for_option_type() {
        StudentGroup group = StudentGroup.builder()
                .id(1L).name("Groupe LD").type(GroupType.OPTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        StudentGroup promo = StudentGroup.builder()
                .id(50L).name("Promo 2026").type(GroupType.PROMOTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        StudentGroupCreateDto dto = new StudentGroupCreateDto();
        dto.setName("Groupe LD");
        dto.setPromotionId(50L);

        StudentGroup saved = StudentGroup.builder()
                .id(1L).name("Groupe LD").type(GroupType.OPTION)
                .category(category).createdBy(creator).promotion(promo).members(new HashSet<>()).build();

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(studentGroupRepository.existsByNameIgnoreCaseAndTypeAndIdNot("Groupe LD", GroupType.OPTION, 1L))
                .thenReturn(false);
        when(studentGroupRepository.findById(50L)).thenReturn(Optional.of(promo));
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(saved);

        StudentGroupDto result = studentGroupService.update(1L, dto, "teacher@teacher.fr");

        assertEquals("Promo 2026", result.getPromotionName());
        assertEquals(50L, result.getPromotionId());
    }

    @Test
    void update_should_clear_promotion_when_promotionId_null_for_option_type() {
        StudentGroup promo = StudentGroup.builder()
                .id(50L).name("Promo 2026").type(GroupType.PROMOTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        StudentGroup group = StudentGroup.builder()
                .id(1L).name("Groupe LD").type(GroupType.OPTION)
                .category(category).createdBy(creator).promotion(promo).members(new HashSet<>()).build();

        StudentGroupCreateDto dto = new StudentGroupCreateDto();
        dto.setName("Groupe LD");
        dto.setPromotionId(null);

        StudentGroup saved = StudentGroup.builder()
                .id(1L).name("Groupe LD").type(GroupType.OPTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(studentGroupRepository.existsByNameIgnoreCaseAndTypeAndIdNot("Groupe LD", GroupType.OPTION, 1L))
                .thenReturn(false);
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(saved);

        StudentGroupDto result = studentGroupService.update(1L, dto, "teacher@teacher.fr");

        assertNull(result.getPromotionName());
        assertNull(result.getPromotionId());
    }

    // -------------------------------------------------------
    // importStudents()
    // -------------------------------------------------------

    @Test
    void importStudents_should_create_new_student_from_csv() throws IOException {
        StudentGroup group = StudentGroup.builder()
                .id(1L).name("Promo 2026").type(GroupType.PROMOTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        Role studentRole = new Role("STUDENT");
        User newUser = new User();
        newUser.setId(100L);
        newUser.setName("Alice Dupont");
        newUser.setEmail("alice@test.fr");
        newUser.setRoles(Set.of(studentRole));

        String csv = "nom,email\nAlice Dupont,alice@test.fr\n";
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("students.csv");
        when(file.getInputStream())
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmail("alice@test.fr")).thenReturn(Optional.empty());
        when(userRepository.findByNameIgnoreCase("Alice Dupont")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(group);

        StudentImportResultDto result = studentGroupService.importStudents(1L, file);

        assertEquals(1, result.getCreated());
        assertEquals(0, result.getAlreadyExisted());
        assertEquals(0, result.getSkipped());
    }

    @Test
    void importStudents_should_count_existing_when_email_already_registered() throws IOException {
        StudentGroup group = StudentGroup.builder()
                .id(1L).name("Promo 2026").type(GroupType.PROMOTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        Role studentRole = new Role("STUDENT");
        User existingUser = new User();
        existingUser.setId(50L);
        existingUser.setName("Bob Martin");
        existingUser.setEmail("bob@test.fr");
        existingUser.setRoles(Set.of(studentRole));

        String csv = "Bob Martin,bob@test.fr\n";
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("students.csv");
        when(file.getInputStream())
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmail("bob@test.fr")).thenReturn(Optional.of(existingUser));
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(group);

        StudentImportResultDto result = studentGroupService.importStudents(1L, file);

        assertEquals(0, result.getCreated());
        assertEquals(1, result.getAlreadyExisted());
        assertEquals(0, result.getSkipped());
    }

    @Test
    void importStudents_should_reuse_existing_user_by_name_when_email_not_found() throws IOException {
        StudentGroup group = StudentGroup.builder()
                .id(1L).name("Promo 2026").type(GroupType.PROMOTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        Role studentRole = new Role("STUDENT");
        User existingByName = new User();
        existingByName.setId(55L);
        existingByName.setName("Carol Simon");
        existingByName.setEmail("carol.other@test.fr");
        existingByName.setRoles(Set.of(studentRole));

        String csv = "Carol Simon,carol@test.fr\n";
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("students.csv");
        when(file.getInputStream())
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmail("carol@test.fr")).thenReturn(Optional.empty());
        when(userRepository.findByNameIgnoreCase("Carol Simon")).thenReturn(Optional.of(existingByName));
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(group);

        StudentImportResultDto result = studentGroupService.importStudents(1L, file);

        assertEquals(0, result.getCreated());
        assertEquals(1, result.getAlreadyExisted());
        assertEquals(0, result.getSkipped());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void importStudents_should_skip_row_with_insufficient_columns() throws IOException {
        StudentGroup group = StudentGroup.builder()
                .id(1L).name("Promo 2026").type(GroupType.PROMOTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        Role studentRole = new Role("STUDENT");
        String csv = "OnlyOneName\n";
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("students.csv");
        when(file.getInputStream())
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(studentRole));
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(group);

        StudentImportResultDto result = studentGroupService.importStudents(1L, file);

        assertEquals(0, result.getCreated());
        assertEquals(0, result.getAlreadyExisted());
        assertEquals(1, result.getSkipped());
    }

    @Test
    void importStudents_should_skip_row_with_invalid_email() throws IOException {
        StudentGroup group = StudentGroup.builder()
                .id(1L).name("Promo 2026").type(GroupType.PROMOTION)
                .category(category).createdBy(creator).members(new HashSet<>()).build();

        Role studentRole = new Role("STUDENT");
        String csv = "Alice Dupont,notavalidemail\n";
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("students.csv");
        when(file.getInputStream())
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(studentRole));
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(group);

        StudentImportResultDto result = studentGroupService.importStudents(1L, file);

        assertEquals(0, result.getCreated());
        assertEquals(0, result.getAlreadyExisted());
        assertEquals(1, result.getSkipped());
    }

    /**
     * A wrong format must be rejected before the group is even looked up.
     */
    @Test
    void importStudents_should_reject_unsupported_format_before_any_lookup() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("students.pdf");

        assertThrows(UnsupportedFileFormatException.class,
                () -> studentGroupService.importStudents(1L, file));

        verifyNoInteractions(studentGroupRepository, userRepository);
    }

    /**
     * An oversized file must be rejected before it is parsed.
     */
    @Test
    void importStudents_should_reject_oversized_file_before_any_lookup() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("students.csv");
        when(file.getSize()).thenReturn(UploadPolicy.MAX_IMPORT_BYTES + 1);

        assertThrows(FileTooLargeException.class,
                () -> studentGroupService.importStudents(1L, file));

        verify(file, never()).getInputStream();
        verifyNoInteractions(studentGroupRepository, userRepository);
    }

    @Test
    void assignTeacher_should_add_teacher_to_group() {
        StudentGroup group = StudentGroup.builder()
                .id(1L).type(GroupType.OPTION).name("Groupe A")
                .category(category).createdBy(creator)
                .members(new HashSet<>()).teachers(new HashSet<>()).build();

        User teacher = new User();
        teacher.setId(20L);
        teacher.setName("M. Dupont");

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userRepository.findById(20L)).thenReturn(Optional.of(teacher));
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(group);

        StudentGroupDto result = studentGroupService.assignTeacher(1L, 20L);

        assertNotNull(result);
        assertTrue(group.getTeachers().contains(teacher));
        verify(studentGroupRepository).save(group);
    }

    @Test
    void assignTeacher_should_throw_when_group_not_found() {
        when(studentGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(StudentGroupNotFoundException.class,
                () -> studentGroupService.assignTeacher(99L, 1L));
    }

    @Test
    void assignTeacher_should_throw_when_teacher_not_found() {
        StudentGroup group = StudentGroup.builder()
                .id(1L).type(GroupType.OPTION).name("Groupe A")
                .category(category).createdBy(creator)
                .members(new HashSet<>()).teachers(new HashSet<>()).build();

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> studentGroupService.assignTeacher(1L, 99L));
    }

    @Test
    void unassignTeacher_should_remove_teacher_from_group() {
        User teacher = new User();
        teacher.setId(20L);
        teacher.setName("M. Dupont");

        Set<User> teachers = new HashSet<>();
        teachers.add(teacher);

        StudentGroup group = StudentGroup.builder()
                .id(1L).type(GroupType.OPTION).name("Groupe A")
                .category(category).createdBy(creator)
                .members(new HashSet<>()).teachers(teachers).build();

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(group);

        StudentGroupDto result = studentGroupService.unassignTeacher(1L, 20L);

        assertNotNull(result);
        assertFalse(group.getTeachers().contains(teacher));
        verify(studentGroupRepository).save(group);
    }

    @Test
    void unassignTeacher_should_throw_when_group_not_found() {
        when(studentGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(StudentGroupNotFoundException.class,
                () -> studentGroupService.unassignTeacher(99L, 1L));
    }

    @Test
    void assignTeacher_project_group_should_assign_to_parent_group() {
        StudentGroup parentGroup = StudentGroup.builder()
                .id(2L).type(GroupType.OPTION).name("Option Parent")
                .category(category).createdBy(creator)
                .members(new HashSet<>()).teachers(new HashSet<>()).build();

        StudentGroup projectGroup = StudentGroup.builder()
                .id(1L).type(GroupType.PROJECT).name("Projet X")
                .category(category).createdBy(creator).parentGroup(parentGroup)
                .members(new HashSet<>()).teachers(new HashSet<>()).build();

        User teacher = new User();
        teacher.setId(20L);
        teacher.setName("M. Dupont");

        when(studentGroupRepository.findById(1L)).thenReturn(Optional.of(projectGroup));
        when(userRepository.findById(20L)).thenReturn(Optional.of(teacher));
        when(studentGroupRepository.save(any(StudentGroup.class))).thenReturn(parentGroup);

        StudentGroupDto result = studentGroupService.assignTeacher(1L, 20L);

        assertNotNull(result);
        assertTrue(parentGroup.getTeachers().contains(teacher));
        verify(studentGroupRepository).save(parentGroup);
    }
}
