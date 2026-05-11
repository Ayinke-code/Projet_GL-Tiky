package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.CategoryRequest;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.CategoryRequestCreateDto;
import fr.horizon.backend_api.model.dto.CategoryRequestDto;
import fr.horizon.backend_api.model.enums.CategoryRequestStatus;
import fr.horizon.backend_api.repository.CategoryRequestRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryRequestService {

    private final CategoryRequestRepository categoryRequestRepository;
    private final UserRepository userRepository;

    /**
     * Récupère toutes les demandes de catégories.
     */
    public List<CategoryRequestDto> findAll() {
        return categoryRequestRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Crée une nouvelle demande de catégorie.
     */
    public CategoryRequestDto create(CategoryRequestCreateDto dto) {

        User requester = userRepository.findById(dto.getRequesterId())
                .orElseThrow(() -> new EntityNotFoundException("Requester not found"));

        CategoryRequest request = CategoryRequest.builder()
                .name(dto.getName())
                .subject(dto.getSubject())
                .description(dto.getDescription())
                .requester(requester)
                .status(CategoryRequestStatus.PENDING)
                .build();

        categoryRequestRepository.save(request);

        return toDto(request);
    }

    /**
     * Supprime une demande par ID.
     */
    public void deleteById(Long id) {
        categoryRequestRepository.deleteById(id);
    }

    /**
     * Convertit entité → DTO.
     */
    private CategoryRequestDto toDto(CategoryRequest request) {
        return CategoryRequestDto.builder()
                .id(request.getId())
                .name(request.getName())
                .subject(request.getSubject())
                .status(request.getStatus().name())
                .requesterId(request.getRequester().getId())
                .requesterName(request.getRequester().getName())
                .description(request.getDescription())
                .build();
    }
}
