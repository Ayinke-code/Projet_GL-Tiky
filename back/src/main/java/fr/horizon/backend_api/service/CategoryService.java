package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryAlreadyExistsException;
import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.CategoryFollower;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.CategoryDto;
import fr.horizon.backend_api.model.dto.CategoryRequestDto;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.repository.CategoryFollowerRepository;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryFollowerRepository followerRepository;
    private final UserRepository userRepository;

    // =========================
    // LECTURE
    // =========================

    @Transactional(readOnly = true)
    public List<CategoryDto> findAll() {
        return categoryRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllActiveCategories() {
        return categoryRepository.findByStatus(CategoryStatus.ACTIVE).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<CategoryDto> getCategoryById(Long id) {
        return categoryRepository.findById(id).map(this::toDto);
    }

    // =========================
    // CRÉATION & MODIFICATION
    // =========================

    @Transactional
    public CategoryDto createCategory(CategoryRequestDto dto, String creatorId) {
        // Validation unicité
        if (categoryRepository.existsByNameIgnoreCaseAndSubjectIgnoreCase(dto.getName(), dto.getSubject())) {
            throw new CategoryAlreadyExistsException("Cette catégorie existe déjà.");
        }

        // Récupération du propriétaire (User object)
        User owner = userRepository.findById(Long.valueOf(creatorId))
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Category category = Category.builder()
                .name(dto.getName().trim())
                .subject(dto.getSubject().trim())
                .status(CategoryStatus.ACTIVE)
                .owner(owner)
                .build();

        Category saved = categoryRepository.save(category);

        // Auto-follow pour le créateur
        followCategory(saved.getId(), creatorId);

        return toDto(saved);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryRequestDto dto, String requesterId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        // Sécurité : Seul l'owner peut modifier
        if (!category.getOwner().getId().toString().equals(requesterId)) {
            throw new SecurityException("Accès refusé.");
        }

        category.setName(dto.getName());
        category.setSubject(dto.getSubject());

        return toDto(categoryRepository.save(category));
    }

    // =========================
    // CYCLE DE VIE (ARCHIVE / DELETE)
    // =========================

    @Transactional
    public CategoryDto archiveCategory(Long id, String requesterId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        category.setStatus(CategoryStatus.ARCHIVED);
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id, String requesterId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        if (category.getStatus() != CategoryStatus.ARCHIVED) {
            throw new IllegalStateException("La catégorie doit être archivée avant suppression.");
        }

        categoryRepository.delete(category);
    }

    // =========================
    // GESTION DES FOLLOWERS
    // =========================

    @Transactional
    public void followCategory(Long categoryId, String teacherId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        if (!followerRepository.existsByTeacherIdAndCategoryId(teacherId, categoryId)) {
            // ACCEPTED si c'est l'owner, sinon PENDING
            String status = category.getOwner().getId().toString().equals(teacherId) ? "ACCEPTED" : "PENDING";

            followerRepository.save(CategoryFollower.builder()
                    .teacherId(teacherId)
                    .categoryId(categoryId)
                    .status(status)
                    .build());
        }
    }

    @Transactional
    public void approveFollower(Long categoryId, String teacherId, String requesterId) {
        CategoryFollower follower = followerRepository.findByTeacherIdAndCategoryId(teacherId, categoryId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        follower.setStatus("ACCEPTED");
        followerRepository.save(follower);
    }

    public List<User> getFollowers(Long categoryId) {
        List<String> teacherIds = followerRepository.findByCategoryId(categoryId).stream()
                .filter(f -> "ACCEPTED".equals(f.getStatus()))
                .map(CategoryFollower::getTeacherId)
                .toList();

        return userRepository.findAllById(teacherIds.stream().map(Long::valueOf).toList());
    }

    // =========================
    // CONVERSION DTO (MAPPER)
    // =========================

    private CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .subject(category.getSubject())
                .status(category.getStatus())
                .ownerName(category.getOwner() != null ? category.getOwner().getName() : "Inconnu")
                .ownerId(category.getOwner() != null ? category.getOwner().getId() : null)
                .build();
    }
}