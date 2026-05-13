package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryAlreadyExistsException;
import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.CategoryFollower;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.CategoryResponseDTO;
import fr.horizon.backend_api.model.dto.CategoryRequestDto;
import fr.horizon.backend_api.model.dto.CreateCategoryDTO;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.repository.CategoryFollowerRepository;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service fusionné gérant les catégories, les followers et la logique métier.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryFollowerRepository followerRepository;
    private final UserRepository userRepository;

    /**
     * Récupère toutes les catégories (Premier code).
     */
    public List<CategoryResponseDTO> findAll() {
        return categoryRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Crée une catégorie à partir de CategoryRequestDto (Premier code).
     */
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDto dto) {
        User owner = userRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Category category = Category.builder()
                .name(dto.getName())
                .subject(dto.getSubject())
                .status(CategoryStatus.ACTIVE)
                .owner(owner)
                .build();

        Category saved = categoryRepository.save(category);
        return convertToDTO(saved);
    }

    /**
     * Supprime une catégorie par ID sans vérification de propriétaire (Premier
     * code).
     */
    public void deleteById(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new CategoryNotFoundException(id);
        }
        categoryRepository.deleteById(id);
    }

    @Transactional
    public CategoryResponseDTO createCategory(CreateCategoryDTO dto, String teacherId) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom est requis");
        }

        if (categoryRepository.existsByNameIgnoreCaseAndSubjectIgnoreCase(dto.getName().trim(),
                dto.getSubject().trim())) {
            throw new CategoryAlreadyExistsException("Cette catégorie existe déjà.");
        }

        User owner = userRepository.findById(Long.valueOf(teacherId))
                .orElseThrow(() -> new RuntimeException("Enseignant non trouvé"));

        Category category = Category.builder()
                .name(dto.getName().trim())
                .subject(dto.getSubject().trim())
                .status(CategoryStatus.ACTIVE)
                .owner(owner)
                .build();

        Category saved = categoryRepository.save(category);

        // Auto-follow pour le créateur
        addFollower(saved.getId(), teacherId);

        return convertToDTO(saved);
    }

    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CreateCategoryDTO dto, String requesterId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        checkOwnership(category, requesterId);

        category.setName(dto.getName().trim());
        category.setSubject(dto.getSubject().trim());

        return convertToDTO(categoryRepository.save(category));
    }

    @Transactional
    public void followCategory(Long categoryId, String teacherId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        if (!followerRepository.existsByTeacherIdAndCategoryId(teacherId, categoryId)) {
            String status = category.getOwner().getId().toString().equals(teacherId) ? "ACCEPTED" : "PENDING";

            followerRepository.save(CategoryFollower.builder()
                    .teacherId(teacherId)
                    .categoryId(categoryId)
                    .status(status)
                    .build());
        }
    }

    @Transactional
    public void unfollowCategory(Long categoryId, String teacherId) {
        followerRepository.deleteByTeacherIdAndCategoryId(teacherId, categoryId);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllActiveCategoriesForTeacher(String teacherId) {
        List<Category> allActive = categoryRepository.findByStatus(CategoryStatus.ACTIVE);
        List<Category> ownedByMe = categoryRepository.findByOwnerId(Long.valueOf(teacherId));

        return Stream.concat(allActive.stream(), ownedByMe.stream())
                .distinct()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponseDTO archiveCategory(Long id, String requesterId) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        checkOwnership(category, requesterId);
        category.setStatus(CategoryStatus.ARCHIVED);
        return convertToDTO(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id, String requesterId) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        checkOwnership(category, requesterId);

        if (category.getStatus() != CategoryStatus.ARCHIVED) {
            throw new IllegalStateException("Seule une catégorie archivée peut être supprimée définitivement.");
        }
        categoryRepository.deleteById(id);
    }

    // === GESTION DES FOLLOWERS ===

    public List<User> getFollowers(Long categoryId) {
        List<String> teacherIds = followerRepository.findByCategoryId(categoryId).stream()
                .filter(f -> "ACCEPTED".equals(f.getStatus()))
                .map(CategoryFollower::getTeacherId)
                .collect(Collectors.toList());

        if (teacherIds.isEmpty())
            return Collections.emptyList();

        return userRepository.findAllById(teacherIds.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList()));
    }

    @Transactional
    public void addFollower(Long categoryId, String teacherId) {
        if (!followerRepository.existsByTeacherIdAndCategoryId(teacherId, categoryId)) {
            followerRepository.save(CategoryFollower.builder()
                    .teacherId(teacherId)
                    .categoryId(categoryId)
                    .status("ACCEPTED")
                    .build());
        }
    }

    @Transactional
    public void approveFollower(Long categoryId, String teacherId, String requesterId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        checkOwnership(category, requesterId);

        CategoryFollower follower = followerRepository.findByTeacherIdAndCategoryId(teacherId, categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Demande non trouvée"));

        follower.setStatus("ACCEPTED");
        followerRepository.save(follower);
    }

    // === UTILITAIRES ET MAPPING ===

    /**
     * Convertit l'entité en DTO (Fusion de toDto et convertToDTO).
     */
    private CategoryResponseDto convertToDTO(Category category) {
        List<User> followers = getFollowers(category.getId());

        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .subject(category.getSubject())
                .status(category.getStatus() != null ? category.getStatus().name() : null)
                .owner(category.getOwner() != null ? category.getOwner().getName() : null)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .followers(new HashSet<>(followers))
                .build();
    }

    private void checkOwnership(Category category, String requesterId) {
        if (category.getOwner() == null || !category.getOwner().getId().toString().equals(requesterId)) {
            throw new SecurityException("Vous n'êtes pas le propriétaire de cette catégorie.");
        }
    }
}