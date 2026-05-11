package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.CategoryRequest;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.CategoryRequestCreateDto;
import fr.horizon.backend_api.model.dto.CategoryRequestDto;
import fr.horizon.backend_api.model.enums.CategoryRequestStatus;
import fr.horizon.backend_api.repository.CategoryRequestRepository;
import fr.horizon.backend_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryRequestServiceTest {

    @Mock
    private CategoryRequestRepository categoryRequestRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryRequestService categoryRequestService;

    @Test
    void shouldReturnAllCategoryRequests() {

        CategoryRequest request = CategoryRequest.builder()
                .id(1L)
                .name("IT")
                .subject("Réseau")
                .status(CategoryRequestStatus.PENDING)
                .requester(User.builder()
                        .id(10L)
                        .name("Alice")
                        .build())
                .description("desc")
                .build();

        when(categoryRequestRepository.findAll())
                .thenReturn(List.of(request));

        List<CategoryRequestDto> result = categoryRequestService.findAll();

        assertEquals(1, result.size());
        assertEquals("IT", result.get(0).getName());
        assertEquals("Alice", result.get(0).getRequesterName());
    }

    @Test
    void shouldCreateCategoryRequest() {

        CategoryRequestCreateDto dto = new CategoryRequestCreateDto();
        dto.setName("IT");
        dto.setSubject("Réseau");
        dto.setRequesterId(1L);
        dto.setDescription("desc");

        User requester = User.builder()
                .id(1L)
                .name("Bob")
                .build();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(requester));

        when(categoryRequestRepository.save(any(CategoryRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryRequestDto result = categoryRequestService.create(dto);

        assertEquals("IT", result.getName());
        assertEquals("Réseau", result.getSubject());
        assertEquals("Bob", result.getRequesterName());
        assertEquals("desc", result.getDescription());

        verify(categoryRequestRepository, times(1)).save(any(CategoryRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenRequesterNotFound() {

        CategoryRequestCreateDto dto = new CategoryRequestCreateDto();
        dto.setRequesterId(99L);

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> categoryRequestService.create(dto));
    }

    @Test
    void shouldDeleteCategoryRequest() {

        Long id = 1L;

        categoryRequestService.deleteById(id);

        verify(categoryRequestRepository, times(1)).deleteById(id);
    }
}
