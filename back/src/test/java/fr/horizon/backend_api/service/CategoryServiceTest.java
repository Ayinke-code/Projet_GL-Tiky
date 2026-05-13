package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryAlreadyExistsException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.CategoryFollower;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.CategoryResponseDTO;
import fr.horizon.backend_api.model.dto.CreateCategoryDTO;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.repository.CategoryFollowerRepository;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryFollowerRepository followerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryService categoryService;

    private CreateCategoryDTO createDTO;
    private Category category;
    private User ownerUser;
    private final String teacherId = "teacher@test.fr";

    @BeforeEach
    void setUp() {
        ownerUser = User.builder()
                .id(1L)
                .email(teacherId)
                .name("Teacher")
                .build();

        createDTO = CreateCategoryDTO.builder()
                .name("Maths")
                .subject("Algèbre")
                .build();

        category = Category.builder()
                .id(1L)
                .name("Maths")
                .subject("Algèbre")
                .status(CategoryStatus.ACTIVE)
                .owner(ownerUser)
                .build();
    }

    @Test
    @DisplayName("Devrait suivre une catégorie")
    void followCategory_Success() {
        when(followerRepository.existsByTeacherIdAndCategoryId(teacherId, 1L)).thenReturn(false);

        categoryService.followCategory(1L, teacherId);

        verify(followerRepository).save(any());
    }

    @Test
    @DisplayName("Ne devrait pas suivre si déjà suivi")
    void followCategory_AlreadyFollowed() {
        when(followerRepository.existsByTeacherIdAndCategoryId(teacherId, 1L)).thenReturn(true);

        categoryService.followCategory(1L, teacherId);

        verify(followerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait désuivre une catégorie")
    void unfollowCategory_Success() {
        categoryService.unfollowCategory(1L, teacherId);
        verify(followerRepository).deleteByTeacherIdAndCategoryId(teacherId, 1L);
    }

    @Test
    @DisplayName("Devrait retourner les IDs des catégories suivies")
    void getFollowedCategoryIds_Success() {
        CategoryFollower follower = CategoryFollower.builder().categoryId(10L).build();
        when(followerRepository.findByTeacherId(teacherId)).thenReturn(List.of(follower));

        List<Long> ids = categoryService.getFollowedCategoryIds(teacherId);

        assertEquals(1, ids.size());
        assertEquals(10L, ids.get(0));
    }

    @Test
    @DisplayName("Devrait créer une catégorie avec succès")
    void createCategory_Success() {
        when(categoryRepository.existsByNameIgnoreCaseAndSubjectIgnoreCase(any(), any())).thenReturn(false);
        when(userRepository.findByEmail(teacherId)).thenReturn(Optional.of(ownerUser));
        when(categoryRepository.save(any())).thenReturn(category);
        when(followerRepository.existsByTeacherIdAndCategoryId(teacherId, category.getId())).thenReturn(false);

        CategoryResponseDTO result = categoryService.createCategory(createDTO, teacherId);

        assertNotNull(result);
        assertEquals("Maths", result.getName());
        verify(categoryRepository).save(any());
        verify(followerRepository).save(any());
    }

    @Test
    @DisplayName("Devrait lever une exception si la catégorie existe déjà")
    void createCategory_AlreadyExists() {
        when(categoryRepository.existsByNameIgnoreCaseAndSubjectIgnoreCase(any(), any())).thenReturn(true);

        assertThrows(CategoryAlreadyExistsException.class, () ->
                categoryService.createCategory(createDTO, teacherId)
        );
    }

    @Test
    @DisplayName("Devrait mettre à jour une catégorie")
    void updateCategory_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        CreateCategoryDTO updateDto = CreateCategoryDTO.builder()
                .name("Maths II")
                .subject("Calcul")
                .build();

        CategoryResponseDTO result = categoryService.updateCategory(1L, updateDto, teacherId);

        assertEquals("Maths II", result.getName());
        assertEquals("Calcul", result.getSubject());
    }

    @Test
    @DisplayName("Devrait lever une exception si mise à jour par un non-propriétaire")
    void updateCategory_SecurityException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThrows(SecurityException.class, () ->
                categoryService.updateCategory(1L, createDTO, "wrong-user")
        );
    }

    @Test
    @DisplayName("Devrait archiver une catégorie")
    void archiveCategory_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        CategoryResponseDTO result = categoryService.archiveCategory(1L, teacherId);

        assertEquals("inactive", result.getStatus());
    }

    @Test
    @DisplayName("Devrait désarchiver une catégorie")
    void unarchiveCategory_Success() {
        category.setStatus(CategoryStatus.INACTIVE);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        CategoryResponseDTO result = categoryService.unarchiveCategory(1L, teacherId);

        assertEquals("active", result.getStatus());
    }

    @Test
    @DisplayName("Devrait supprimer une catégorie archivée")
    void deleteCategory_Success() {
        category.setStatus(CategoryStatus.INACTIVE);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertDoesNotThrow(() -> categoryService.deleteCategory(1L, teacherId));
        verify(categoryRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Ne devrait pas supprimer une catégorie active")
    void deleteCategory_IllegalState() {
        category.setStatus(CategoryStatus.ACTIVE);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThrows(IllegalStateException.class, () ->
                categoryService.deleteCategory(1L, teacherId)
        );
    }

    @Test
    @DisplayName("Devrait récupérer une catégorie par ID")
    void getCategoryById_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Optional<CategoryResponseDTO> result = categoryService.getCategoryById(1L);

        assertTrue(result.isPresent());
        assertEquals("Maths", result.get().getName());
    }

    @Test
    @DisplayName("Devrait récupérer toutes les catégories actives pour un prof")
    void getAllActiveCategoriesForTeacher_Success() {
        when(categoryRepository.findByStatus(CategoryStatus.ACTIVE)).thenReturn(List.of(category));
        when(userRepository.findByEmail(teacherId)).thenReturn(Optional.of(ownerUser));
        when(categoryRepository.findByOwner(ownerUser)).thenReturn(List.of(category));

        List<CategoryResponseDTO> results = categoryService.getAllActiveCategoriesForTeacher(teacherId);

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }
}
