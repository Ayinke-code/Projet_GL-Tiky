package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.DeletionCategoryRequest;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.DeletionCategoryRequestDto;
import fr.horizon.backend_api.model.enums.CategoryRequestStatus;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.DeletionCategoryRequestRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeletionCategoryRequestService {

    private final DeletionCategoryRequestRepository deletionCategoryRequestRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final UserRepository userRepository;

    private static final String USER_NF = "User not found";


    @Transactional
    public DeletionCategoryRequestDto createRequest(Long categoryId, String email) {
        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USER_NF));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        if (!category.getFollowers().contains(requester)) {
            throw new IllegalStateException("Le requérant n'est pas un follower de cette catégorie.");
        }

        boolean exists = deletionCategoryRequestRepository
                .existsByCategoryAndRequesterAndStatus(category, requester, CategoryRequestStatus.PENDING);
        if (exists) {
            throw new IllegalStateException("Une demande de désinscription est déjà en attente pour cette catégorie.");
        }

        DeletionCategoryRequest request = DeletionCategoryRequest.builder()
                .requester(requester)
                .category(category)
                .status(CategoryRequestStatus.PENDING)
                .build();

        return mapToDto(deletionCategoryRequestRepository.save(request));
    }


    @Transactional
    public DeletionCategoryRequestDto acceptRequest(Long requestId, String email) {
        DeletionCategoryRequest request = getRequestById(requestId);

        User validator = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USER_NF));

        if (request.getStatus() != CategoryRequestStatus.PENDING) {
            throw new IllegalStateException("Seules les demandes PENDING peuvent être acceptées.");
        }

        // Appel de votre méthode existante pour retirer concrètement le follower
        categoryService.deleteFollowerById(request.getCategory().getId(), request.getRequester().getId());

        // Mise à jour de la demande
        request.setStatus(CategoryRequestStatus.APPROVED);
        request.setValidator(validator);

        return mapToDto(deletionCategoryRequestRepository.save(request));
    }

    @Transactional
    public DeletionCategoryRequestDto rejectRequest(Long requestId, String email, String rejectionReason) {
        DeletionCategoryRequest request = getRequestById(requestId);

        User validator = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USER_NF));

        if (request.getStatus() != CategoryRequestStatus.PENDING) {
            throw new IllegalStateException("Seules les demandes PENDING peuvent être refusées.");
        }

        request.setStatus(CategoryRequestStatus.REJECTED);
        request.setValidator(validator);
        request.setRejectionReason(rejectionReason);

        return mapToDto(deletionCategoryRequestRepository.save(request));
    }


    @Transactional(readOnly = true)
    public List<DeletionCategoryRequestDto> getPendingRequestsForManager(Long managerId) {
        return deletionCategoryRequestRepository.findByCategory_Owner_IdAndStatus(managerId, CategoryRequestStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }


    private DeletionCategoryRequestDto mapToDto(DeletionCategoryRequest entity) {
        if (entity == null) return null;

        return DeletionCategoryRequestDto.builder()
                .id(entity.getId())
                .requesterId(entity.getRequester() != null ? entity.getRequester().getId() : null)
                .validatorId(entity.getValidator() != null ? entity.getValidator().getId() : null)
                .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
                .status(entity.getStatus().name())
                .rejectionReason(entity.getRejectionReason())
                .build();
    }

    private DeletionCategoryRequest getRequestById(Long requestId) {
        return deletionCategoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable avec l'id: " + requestId));
    }
}