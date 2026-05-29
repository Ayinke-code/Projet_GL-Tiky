package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.CategoryRequest;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.CategoryRequestCreateDto;
import fr.horizon.backend_api.model.dto.CategoryRequestDto;
import fr.horizon.backend_api.model.enums.CategoryRequestStatus;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.CategoryRequestRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de gestion des demandes de création de catégories.
 * <p>
 * Fournit les opérations métier relatives aux demandes :
 * création, récupération, suppression, validation et conversion en DTO.
 * <p>
 * La validation d’une demande crée automatiquement la catégorie correspondante
 * et met à jour le statut de la demande à {@code APPROVED}.
 */
@Service
@RequiredArgsConstructor
public class CategoryRequestService {

    private final CategoryRequestRepository categoryRequestRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Récupère toutes les demandes de catégories.
     *
     * @return liste de DTO de toutes les demandes
     */
    public List<CategoryRequestDto> findAll() {
        return categoryRequestRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Crée une nouvelle demande de catégorie.
     *
     * @param dto données de création (nom, sujet, description, identifiant du demandeur)
     * @return demande créée sous forme de DTO
     * @throws EntityNotFoundException si le demandeur n’existe pas
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
     * Supprime une demande de catégorie par identifiant.
     *
     * @param id identifiant de la demande
     */
    public void deleteById(Long id) {
        categoryRequestRepository.deleteById(id);
    }

    /**
     * Valide une demande de catégorie et crée la catégorie correspondante.
     * <p>
     * Met à jour le statut de la demande à {@code APPROVED},
     * associe le validateur et lie la catégorie créée.
     *
     * @param id            identifiant de la demande à valider
     * @param validatorName email de l’utilisateur validant la demande
     * @throws EntityNotFoundException si la demande ou le validateur n’existe pas
     */
    @Transactional
    public void accept(Long id, String validatorName) {
        CategoryRequest request = categoryRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category request not found"));

        User validator = userRepository.findByEmail(validatorName)
                .orElseThrow(() -> new EntityNotFoundException("Validator not found"));

        // status initalisé à ACTIVE par défaut
        Category category = Category.builder()
                .name(request.getName())
                .subject(request.getSubject())
                .owner(validator)
                .build();

        // Créer la catégorie
        categoryRepository.save(category);

        request.setStatus(CategoryRequestStatus.APPROVED);
        request.setValidator(validator);
        request.setCategory(category);

        // Met à jour la demande
        categoryRequestRepository.save(request);
    }

    /**
     * Convertit une entité {@link CategoryRequest} en DTO.
     *
     * @param request entité demande de catégorie à convertir
     * @return DTO contenant les informations publiques de la demande
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
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
