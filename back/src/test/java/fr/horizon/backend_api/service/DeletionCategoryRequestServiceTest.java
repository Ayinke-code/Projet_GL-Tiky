package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.DeletionCategoryRequestDto;
import fr.horizon.backend_api.model.enums.CategoryRequestStatus;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.DeletionCategoryRequestRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeletionCategoryRequestServiceTest {

    @Mock
    private DeletionCategoryRequestRepository deletionCategoryRequestRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeletionCategoryRequestService service;

    private static final String EMAIL = "test@test.com";


    @Test
    @DisplayName("createRequest - success when user is follower and no pending request")
    void createRequest_success() {
        Long categoryId = 1L;

        User user = User.builder().id(10L).email(EMAIL).build();
        Category category = mock(Category.class);
        DeletionCategoryRequest saved = DeletionCategoryRequest.builder()
                .id(100L)
                .status(CategoryRequestStatus.PENDING)
                .requester(user)
                .category(category)
                .build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(category.getFollowers()).thenReturn(Set.of(user));
        when(deletionCategoryRequestRepository.existsByCategoryAndRequesterAndStatus(
                category, user, CategoryRequestStatus.PENDING)).thenReturn(false);
        when(deletionCategoryRequestRepository.save(any())).thenReturn(saved);

        DeletionCategoryRequestDto result = service.createRequest(categoryId, EMAIL);

        assertNotNull(result);

        verify(userRepository).findByEmail(EMAIL);
        verify(categoryRepository).findById(categoryId);
        verify(deletionCategoryRequestRepository)
                .existsByCategoryAndRequesterAndStatus(category, user, CategoryRequestStatus.PENDING);
        verify(deletionCategoryRequestRepository).save(any(DeletionCategoryRequest.class));
    }

    @Test
    @DisplayName("createRequest - throws when user not follower")
    void createRequest_notFollower() {
        Long categoryId = 1L;

        User user = User.builder().id(10L).build();
        Category category = mock(Category.class);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(category.getFollowers()).thenReturn(Set.of());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.createRequest(categoryId, EMAIL)
        );

        assertEquals("Le requérant n'est pas un follower de cette catégorie.", ex.getMessage());

        verify(userRepository).findByEmail(EMAIL);
        verify(categoryRepository).findById(categoryId);
        verifyNoInteractions(deletionCategoryRequestRepository);
    }


    @Test
    @DisplayName("acceptRequest - success approves request and removes follower")
    void acceptRequest_success() {
        Long requestId = 1L;

        User requester = User.builder().id(2L).build();
        User validator = User.builder().id(3L).email(EMAIL).build();

        Category category = mock(Category.class);

        DeletionCategoryRequest request = DeletionCategoryRequest.builder()
                .id(requestId)
                .status(CategoryRequestStatus.PENDING)
                .requester(requester)
                .category(category)
                .build();

        when(deletionCategoryRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(validator));
        when(deletionCategoryRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        when(category.getId()).thenReturn(99L);

        DeletionCategoryRequestDto result = service.acceptRequest(requestId, EMAIL);

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());

        verify(categoryService).deleteFollowerById(99L, requester.getId());
        verify(deletionCategoryRequestRepository).save(request);
        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("acceptRequest - throws when status is not PENDING")
    void acceptRequest_invalidStatus() {

        Long requestId = 1L;

        DeletionCategoryRequest request = DeletionCategoryRequest.builder()
                .status(CategoryRequestStatus.REJECTED)
                .build();

        when(deletionCategoryRequestRepository.findById(requestId))
                .thenReturn(Optional.of(request));

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(User.builder()
                        .id(1L)
                        .email(EMAIL)
                        .build()));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.acceptRequest(requestId, EMAIL)
        );

        assertEquals("Seules les demandes PENDING peuvent être acceptées.", ex.getMessage());

        verify(deletionCategoryRequestRepository).findById(requestId);
        verify(userRepository).findByEmail(EMAIL);

        verifyNoInteractions(categoryService);
    }


    @Test
    @DisplayName("rejectRequest - success sets REJECTED status and reason")
    void rejectRequest_success() {
        Long requestId = 1L;

        User validator = User.builder().id(5L).email(EMAIL).build();

        DeletionCategoryRequest request = DeletionCategoryRequest.builder()
                .id(requestId)
                .status(CategoryRequestStatus.PENDING)
                .build();

        when(deletionCategoryRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(validator));
        when(deletionCategoryRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DeletionCategoryRequestDto result =
                service.rejectRequest(requestId, EMAIL, "Not valid");

        assertNotNull(result);
        assertEquals("REJECTED", result.getStatus());
        assertEquals("Not valid", result.getRejectionReason());

        verify(deletionCategoryRequestRepository).save(request);
        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("hasPendingRequest - returns true when exists")
    void hasPendingRequest_true() {
        Long categoryId = 1L;

        User user = User.builder().id(10L).email(EMAIL).build();
        Category category = mock(Category.class);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(deletionCategoryRequestRepository
                .existsByCategoryAndRequesterAndStatus(category, user, CategoryRequestStatus.PENDING))
                .thenReturn(true);

        boolean result = service.hasPendingRequest(categoryId, EMAIL);

        assertTrue(result);

        verify(deletionCategoryRequestRepository)
                .existsByCategoryAndRequesterAndStatus(category, user, CategoryRequestStatus.PENDING);
    }


    @Test
    @DisplayName("createRequest - throws when user not found")
    void userNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.createRequest(1L, EMAIL));

        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("createRequest - throws when category not found")
    void categoryNotFound() {
        User user = User.builder().id(1L).email(EMAIL).build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class,
                () -> service.createRequest(1L, EMAIL));
    }
}