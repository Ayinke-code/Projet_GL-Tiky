package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNameAlreadyExistsOfThisTypeException;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.*;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.model.enums.RoleEnum;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.CategoryRequestRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service de gestion des catégories.
 * <p>
 * Fournit les opérations métier liées aux catégories :
 * création, récupération (avec filtrage par type d’événement),
 * suppression en cascade (issues + demandes associées) et conversion en DTO.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryRequestRepository categoryRequestRepository;
    private final IssueRepository issueRepository;
    private final IssueService issueService;

    /**
     * Récupère toutes les catégories.
     *
     * @return liste de DTO de toutes les catégories
     */
    public List<CategoryDto> findAll() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Récupère une catégorie par son identifiant.
     *
     * @param id identifiant de la catégorie
     * @return DTO détaillé de la catégorie
     * @throws CategoryNotFoundException si la catégorie n'existe pas
     */
    public CategoryDetailsDto findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        return toDetailsDto(category);
    }

    /**
     * Crée une nouvelle catégorie.
     *
     * @param dto données de création (nom, sujet, identifiant du propriétaire)
     * @return catégorie créée sous forme de DTO
     * @throws EntityNotFoundException si le propriétaire n'existe pas
     */
    public CategoryDto createCategory(CategoryCreateDto dto) {

        User owner = userRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));

        // Check si une catégorie n'existe pas déja avec ce nom et de type ISSUE
        if (categoryRepository.existsByNameIgnoreCaseAndCategoryType(
                dto.getName(),
                CategoryType.ISSUE)) {
            throw new CategoryNameAlreadyExistsOfThisTypeException(dto.getName());
        }

        Category category = Category.builder()
                .name(dto.getName())
                .subject(dto.getSubject())
                //correction
                .categoryType(CategoryType.ISSUE)
                .status(CategoryStatus.ACTIVE)
                .owner(owner)
                .build();

        categoryRepository.save(category);

        return toDto(category);
    }

    /**
     * Retire un follower d'une catégorie.
     */
    @Transactional
    public void deleteFollowerById(Long idCategory, Long idUser) {

        User user = userRepository.findById(idUser)
                .orElseThrow(() -> new EntityNotFoundException("User not found " + idUser));

        Category category = categoryRepository.findById(idCategory)
                .orElseThrow(() -> new CategoryNotFoundException(idCategory));

        boolean isTeacher = user.getRoles()
                .stream()
                .anyMatch(role -> role.getName().equals(RoleEnum.TEACHER.value()));

        if (!isTeacher) {
            throw new IllegalArgumentException("Only teachers can be followers");
        }

        if (!category.getFollowers().contains(user)) {
            throw new IllegalStateException("User is not a follower");
        }

        category.getFollowers().remove(user);

        categoryRepository.save(category);
    }

    /**
     * Ajoute un follower à une catégorie.
     */
    @Transactional
    public void addFollower(Long idCategory, Long idUser) {

        User user = userRepository.findById(idUser)
                .orElseThrow(() -> new EntityNotFoundException("User not found " + idUser));

        Category category = categoryRepository.findById(idCategory)
                .orElseThrow(() -> new CategoryNotFoundException(idCategory));

        boolean isTeacher = user.getRoles()
                .stream()
                .anyMatch(role -> role.getName().equals(RoleEnum.TEACHER.value()));

        if (!isTeacher) {
            throw new IllegalArgumentException("Only teachers can be followers");
        }

        if (category.getFollowers().contains(user)) {
            throw new IllegalStateException("User already follows this category");
        }

        category.getFollowers().add(user);

        categoryRepository.save(category);
    }

    /**
     * Supprime une catégorie et toutes les entités associées (issues et demandes).
     *
     * @param id identifiant de la catégorie
     * @throws CategoryNotFoundException si la catégorie n’existe pas
     */
    @Transactional
    public void deleteById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        // Délègue la suppression des issues et leurs dépendances
        issueService.deleteAllByCategory(category);

        // Supprime les demandes de catégorie
        categoryRequestRepository.deleteAllByCategory(category);

        categoryRepository.delete(category);
    }

    /**
     * Récupère les catégories filtrées par type d’événement.
     *
     * @param categoryType type ({@code AUDIT}, {@code DEFENSE}, {@code MEETING} ou {@code ISSUE})
     * @return liste de DTO des catégories correspondant au type
     */
    public List<CategoryDto> findByCategoryType(CategoryType categoryType) {
        return categoryRepository.findByCategoryType(categoryType)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Convertit une entité {@link Category} en DTO.
     *
     * @param category entité catégorie à convertir
     * @return DTO contenant les informations publiques de la catégorie
     */
    private CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .subject(category.getSubject())
                .status(category.getStatus())
                .categoryType(category.getCategoryType())
                .ownerName(category.getOwner() != null ? category.getOwner().getName() : null)
                .ownerId(category.getOwner() != null ? category.getOwner().getId() : null)
                .build();
    }

    /**
     * Convertit l'entité User → DTO comprenant: id, name, roles.
     */
    private UserRefDto toUserRefDto(User user, Category category) {

        /**
         * Vérifie si l'utilisateur est manager d'au moins une issue
         * ouverte appartenant à la catégorie courante.
         */
        boolean isIssueManagerInCategory = issueRepository
                .existsByManagerIdAndStatusAndCategoryId(
                        user.getId(),
                        IssueStatus.OUVERT,
                        category.getId()
                );

        return UserRefDto.builder()
                .id(user.getId())
                .name(user.getName())
                .roles(
                        user.getRoles()
                                .stream()
                                .map(Role::getName)
                                .collect(Collectors.toSet())
                )
                .isIssueManagerInCategory(isIssueManagerInCategory)
                .build();
    }

    /**
     * Convertit l'entité Category et User → DTO comprenant: CategoryDto et une liste de UserRefDto.
     */
    public CategoryDetailsDto toDetailsDto(Category category) {
        return CategoryDetailsDto.builder()
                .category(toDto(category))
                .followers(
                        category.getFollowers()
                                .stream()
                                .map(user -> toUserRefDto(user, category))
                                .toList()
                )
                .build();
    }
}
