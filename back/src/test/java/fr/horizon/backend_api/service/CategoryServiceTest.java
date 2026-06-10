package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.CategoryDetailsDto;
import fr.horizon.backend_api.model.dto.CategoryDto;
import fr.horizon.backend_api.model.dto.CategoryCreateDto;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRequestRepository categoryRequestRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private IssueService issueService;

    @Mock
    private DeletionCategoryRequestRepository deletionCategoryRequestRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CategoryService categoryService;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .id(1L)
                .name("Lucas")
                .email("lucas@test.com")
                .build();

        category = Category.builder()
                .id(10L)
                .name("DEV")
                .subject("Développement")
                .status(CategoryStatus.ACTIVE)
                .owner(user)
                .build();
    }

    @Test
    void shouldFindAllCategories() {

        when(categoryRepository.findAll())
                .thenReturn(List.of(category));

        List<CategoryDto> result = categoryService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("DEV");
        assertThat(result.get(0).getSubject()).isEqualTo("Développement");

        verify(categoryRepository).findAll();
    }

    @Test
    void shouldCreateCategory() {

        CategoryCreateDto dto = CategoryCreateDto.builder()
                .name("JAVA")
                .subject("Backend Java")
                .ownerId(1L)
                .build();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryDto result = categoryService.createCategory(dto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("JAVA");
        assertThat(result.getSubject()).isEqualTo("Backend Java");

        verify(userRepository).findById(1L);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void shouldThrowExceptionWhenOwnerNotFound() {

        CategoryCreateDto dto = CategoryCreateDto.builder()
                .name("JAVA")
                .subject("Backend")
                .ownerId(999L)
                .build();

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.createCategory(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Owner not found");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldDeleteCategory() {

        when(categoryRepository.findById(10L))
                .thenReturn(Optional.of(category));

        categoryService.deleteById(10L);

        verify(issueService).deleteAllByCategory(category);
        verify(categoryRequestRepository).deleteAllByCategory(category);
        verify(categoryRepository).delete(category);
    }

    @Test
    void shouldThrowExceptionWhenCategoryNotFound() {

        when(categoryRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteById(999L))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category not found with id: 999");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void shouldFindCategoryById() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        CategoryDetailsDto result = categoryService.findById(10L);

        assertThat(result).isNotNull();
        assertThat(result.getCategory().getId()).isEqualTo(10L);

        verify(categoryRepository).findById(10L);
    }

    @Test
    void shouldThrowWhenCategoryNotFoundInFindById() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(999L))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void shouldAddFollower_WhenManagerAddsTeacher() {
        when(userRepository.findByEmail("lucas@test.com")).thenReturn(Optional.of(user));

        User teacherToAdd = User.builder().id(2L).name("Prof").roles(Set.of(Role.builder().name("TEACHER").build())).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(teacherToAdd));

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        categoryService.addFollower(10L, 2L, "lucas@test.com");

        assertThat(category.getFollowers()).contains(teacherToAdd);
        verify(categoryRepository).save(category);

        verify(notificationService).managerAddingFollowerNotification(teacherToAdd, category);
    }

    @Test
    void shouldAddFollower_WhenTeacherFollowsByHimself() {
        // Le prof s'abonne lui-même (l'initiateur est le prof)
        User teacher = User.builder().id(2L).name("Prof").email("prof@test.com").roles(Set.of(Role.builder().name("TEACHER").build())).build();

        when(userRepository.findByEmail("prof@test.com")).thenReturn(Optional.of(teacher));
        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        categoryService.addFollower(10L, 2L, "prof@test.com");

        assertThat(category.getFollowers()).contains(teacher);
        verify(categoryRepository).save(category);

        // 👉 On vérifie que l'autre notification est envoyée (au manager)
        verify(notificationService).notifyNewFollowerCategory(teacher, category);
    }

    @Test
    void shouldThrowWhenUserNotFoundAddFollower() {
        // On suppose que l'initiateur existe
        when(userRepository.findByEmail("lucas@test.com")).thenReturn(Optional.of(user));
        // Mais la cible n'existe pas
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.addFollower(10L, 2L, "lucas@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shouldThrowWhenInitiatorNotFoundAddFollower() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.addFollower(10L, 1L, "ghost@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shouldThrowWhenCategoryNotFoundAddFollower() {
        when(userRepository.findByEmail("lucas@test.com")).thenReturn(Optional.of(user));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.addFollower(10L, 1L, "lucas@test.com"))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void shouldThrowWhenUserIsNotTeacherAddFollower() {
        when(userRepository.findByEmail("lucas@test.com")).thenReturn(Optional.of(user));

        User student = User.builder().id(2L).roles(Set.of(Role.builder().name("STUDENT").build())).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.addFollower(10L, 2L, "lucas@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only teachers");
    }

    @Test
    void shouldThrowWhenAlreadyFollower() {
        when(userRepository.findByEmail("lucas@test.com")).thenReturn(Optional.of(user));

        User teacher = User.builder().id(2L).roles(Set.of(Role.builder().name("TEACHER").build())).build();
        category.getFollowers().add(teacher); // Déjà dedans

        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.addFollower(10L, 2L, "lucas@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already");
    }

    // =============================================
    // findByCategoryType()
    // =============================================

    /**
     * Vérifie que les catégories de type AUDIT sont retournées et correctement mappées.
     */
    @Test
    void findByCategoryType_should_return_audit_categories() {

        // GIVEN
        Category auditCategory = Category.builder()
                .id(11L)
                .name("Test")
                .subject("Audit Test")
                .status(CategoryStatus.ACTIVE)
                .categoryType(CategoryType.AUDIT)
                .owner(user)
                .build();

        when(categoryRepository.findByCategoryType(CategoryType.AUDIT))
                .thenReturn(List.of(auditCategory));

        // WHEN
        List<CategoryDto> result = categoryService.findByCategoryType(CategoryType.AUDIT);

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test");
        assertThat(result.get(0).getSubject()).isEqualTo("Audit Test");
        assertThat(result.get(0).getCategoryType()).isEqualTo(CategoryType.AUDIT);
        assertThat(result.get(0).getOwnerName()).isEqualTo("Lucas");

        verify(categoryRepository).findByCategoryType(CategoryType.AUDIT);
    }

    /**
     * Vérifie que les catégories de type DEFENSE sont retournées.
     */
    @Test
    void findByCategoryType_should_return_defense_categories() {

        // GIVEN
        Category defenseCategory = Category.builder()
                .id(20L)
                .name("PFE")
                .subject("Projet Fin Études")
                .status(CategoryStatus.ACTIVE)
                .categoryType(CategoryType.DEFENSE)
                .owner(user)
                .build();

        when(categoryRepository.findByCategoryType(CategoryType.DEFENSE))
                .thenReturn(List.of(defenseCategory));

        // WHEN
        List<CategoryDto> result = categoryService.findByCategoryType(CategoryType.DEFENSE);

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryType()).isEqualTo(CategoryType.DEFENSE);
        assertThat(result.get(0).getName()).isEqualTo("PFE");

        verify(categoryRepository).findByCategoryType(CategoryType.DEFENSE);
    }

    /**
     * Vérifie qu'une liste vide est retournée si aucune catégorie ne correspond au type.
     */
    @Test
    void findByCategoryType_should_return_empty_when_no_match() {

        // GIVEN
        when(categoryRepository.findByCategoryType(CategoryType.AUDIT))
                .thenReturn(List.of());

        // WHEN
        List<CategoryDto> result = categoryService.findByCategoryType(CategoryType.AUDIT);

        // THEN
        assertThat(result).isEmpty();
        verify(categoryRepository).findByCategoryType(CategoryType.AUDIT);
    }

    @Test
    void shouldDeleteFollower() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        user.setRoles(Set.of(Role.builder().name("TEACHER").build()));
        category.getFollowers().add(user);

        categoryService.deleteFollowerById(10L, 1L);

        assertThat(category.getFollowers()).doesNotContain(user);

        verify(categoryRepository).save(category);
    }

    @Test
    void shouldThrowWhenUserNotFoundDeleteFollower() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteFollowerById(10L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shouldThrowWhenCategoryNotFoundDeleteFollower() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteFollowerById(10L, 1L))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void shouldThrowWhenUserNotTeacherDeleteFollower() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        user.setRoles(Set.of(Role.builder().name("STUDENT").build()));

        assertThatThrownBy(() -> categoryService.deleteFollowerById(10L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenUserIsNotFollower() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        user.setRoles(Set.of(Role.builder().name("TEACHER").build()));

        assertThatThrownBy(() -> categoryService.deleteFollowerById(10L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a follower");
    }
}