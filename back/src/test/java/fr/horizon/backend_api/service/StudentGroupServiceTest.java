package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.InvalidGroupSizeException;
import fr.horizon.backend_api.exception.StudentAlreadyInGroupException;
import fr.horizon.backend_api.exception.UserNotFoundException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.StudentGroup;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.StudentGroupCreateDto;
import fr.horizon.backend_api.model.dto.StudentGroupDto;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.model.enums.GroupType;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.StudentGroupRepository;
import fr.horizon.backend_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

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

        studentGroupService.deleteById(1L);

        verify(studentGroupRepository).findById(1L);
        verify(studentGroupRepository).delete(group);
    }

    @Test
    void deleteById_should_throw_StudentGroupNotFoundException_when_not_found() {

        when(studentGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(fr.horizon.backend_api.exception.StudentGroupNotFoundException.class,
                () -> studentGroupService.deleteById(99L));

        verify(studentGroupRepository, never()).delete(any());
    }
}
