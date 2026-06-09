package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.CategoryDetailsDto;
import fr.horizon.backend_api.model.dto.CategoryDto;
import fr.horizon.backend_api.model.dto.CategoryCreateDto;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.CategoryRequestRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.UserRepository;
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

        when(userRepository.findById(any()))
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
    void shouldAddFollower() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        user.setRoles(Set.of(Role.builder().name("TEACHER").build()));

        categoryService.addFollower(10L, "1");

        assertThat(category.getFollowers()).contains(user);

        verify(categoryRepository).save(category);
    }

    @Test
    void shouldThrowWhenUserNotFoundAddFollower() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.addFollower(10L, "1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shouldThrowWhenCategoryNotFoundAddFollower() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.addFollower(10L, "1"))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void shouldAddFollowerWhenUserIsStudent() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        user.setRoles(Set.of(Role.builder().name("STUDENT").build()));

        categoryService.addFollower(10L, "1");

        assertThat(category.getFollowers()).contains(user);
        verify(categoryRepository).save(category);
    }

    @Test
    void shouldThrowWhenUserHasInvalidRoleAddFollower() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        user.setRoles(Set.of(Role.builder().name("GUEST").build()));

        assertThatThrownBy(() -> categoryService.addFollower(10L, "1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only teachers, admins or students");
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
    void shouldThrowWhenAlreadyFollower() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        user.setRoles(Set.of(Role.builder().name("TEACHER").build()));
        category.getFollowers().add(user);

        assertThatThrownBy(() -> categoryService.addFollower(10L, "1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already");
    }

    @Test
    void shouldDeleteFollower() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        user.setRoles(Set.of(Role.builder().name("TEACHER").build()));
        category.getFollowers().add(user);

        categoryService.deleteFollowerById(10L, "1");

        assertThat(category.getFollowers()).doesNotContain(user);

        verify(categoryRepository).save(category);
    }

    @Test
    void shouldThrowWhenUserNotFoundDeleteFollower() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteFollowerById(10L, "1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shouldThrowWhenCategoryNotFoundDeleteFollower() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteFollowerById(10L, "1"))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void shouldDeleteFollowerWhenUserIsStudent() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        user.setRoles(Set.of(Role.builder().name("STUDENT").build()));
        category.getFollowers().add(user);

        categoryService.deleteFollowerById(10L, "1");

        assertThat(category.getFollowers()).doesNotContain(user);
        verify(categoryRepository).save(category);
    }

    @Test
    void shouldThrowWhenUserHasInvalidRoleDeleteFollower() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        user.setRoles(Set.of(Role.builder().name("GUEST").build()));

        assertThatThrownBy(() -> categoryService.deleteFollowerById(10L, "1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only teachers, admins or students");
    }

    @Test
    void shouldThrowWhenUserIsNotFollower() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        user.setRoles(Set.of(Role.builder().name("TEACHER").build()));

        assertThatThrownBy(() -> categoryService.deleteFollowerById(10L, "1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a follower");
    }
}