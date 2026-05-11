package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.CategoryDto;
import fr.horizon.backend_api.model.dto.CategoryCreateDto;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

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

    /**
     * Vérifie que toutes les catégories sont récupérées.
     */
    @Test
    void shouldFindAllCategories() {

        // GIVEN
        when(categoryRepository.findAll())
                .thenReturn(List.of(category));

        // WHEN
        List<CategoryDto> result = categoryService.findAll();

        // THEN
        assertThat(result).hasSize(1);

        assertThat(result.get(0).getName())
                .isEqualTo("DEV");

        assertThat(result.get(0).getSubject())
                .isEqualTo("Développement");

        verify(categoryRepository).findAll();
    }

    /**
     * Vérifie qu'une catégorie est correctement créée.
     */
    @Test
    void shouldCreateCategory() {

        // GIVEN
        CategoryCreateDto dto = CategoryCreateDto.builder()
                .name("JAVA")
                .subject("Backend Java")
                .ownerId(1L)
                .build();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        CategoryDto result = categoryService.createCategory(dto);

        // THEN
        assertThat(result).isNotNull();

        assertThat(result.getName())
                .isEqualTo("JAVA");

        assertThat(result.getSubject())
                .isEqualTo("Backend Java");

        verify(userRepository).findById(1L);
        verify(categoryRepository).save(any(Category.class));
    }

    /**
     * Vérifie qu'une erreur est levée si le owner n'existe pas.
     */
    @Test
    void shouldThrowExceptionWhenOwnerNotFound() {

        // GIVEN
        CategoryCreateDto dto = CategoryCreateDto.builder()
                .name("JAVA")
                .subject("Backend")
                .ownerId(999L)
                .build();

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> categoryService.createCategory(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Owner not found");

        verify(userRepository).findById(999L);
        verify(categoryRepository, never()).save(any());
    }

    /**
     * Vérifie qu'une catégorie est supprimée.
     */
    @Test
    void shouldDeleteCategory() {

        // GIVEN
        when(categoryRepository.existsById(10L))
                .thenReturn(true);

        // WHEN
        categoryService.deleteById(10L);

        // THEN
        verify(categoryRepository).deleteById(10L);
    }

    /**
     * Vérifie qu'une erreur est levée si la catégorie n'existe pas.
     */
    @Test
    void shouldThrowExceptionWhenCategoryNotFound() {

        // GIVEN
        when(categoryRepository.existsById(999L))
                .thenReturn(false);

        // WHEN / THEN
        assertThatThrownBy(() -> categoryService.deleteById(999L))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category not found with id: 999");

        verify(categoryRepository, never()).deleteById(anyLong());
    }
}
