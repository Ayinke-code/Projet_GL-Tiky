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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service pour gérer les opérations métier des catégories.
 *
 * @author Tiky Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryFollowerRepository followerRepository;
    private final UserRepository userRepository;

    // ========================= FOLLOW / UNFOLLOW =========================

    @Transactional
    public void followCategory(Long categoryId, String teacherEmail) {
        if (!followerRepository.existsByTeacherIdAndCategoryId(teacherEmail, categoryId)) {
            followerRepository.save(CategoryFollower.builder()
                    .teacherId(teacherEmail)
                    .categoryId(categoryId)
                    .build());
        }
    }

    @Transactional
    public void unfollowCategory(Long categoryId, String teacherEmail) {
        followerRepository.deleteByTeacherIdAndCategoryId(teacherEmail, categoryId);
    }

    public List<Long> getFollowedCategoryIds(String teacherEmail) {
        return followerRepository.findByTeacherId(teacherEmail).stream()
                .map(CategoryFollower::getCategoryId)
                .collect(Collectors.toList());
    }

    // ========================= CRUD =========================

    @Transactional
    public CategoryResponseDTO createCategory(CreateCategoryDTO dto, String teacherEmail) {
        if (dto.getName() == null || dto.getName().trim().isEmpty() ||
                dto.getSubject() == null || dto.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom et le sujet de la catégorie sont requis");
        }

        if (categoryRepository.existsByNameIgnoreCaseAndSubjectIgnoreCase(dto.getName().trim(), dto.getSubject().trim())) {
            throw new CategoryAlreadyExistsException(
                    "La catégorie '" + dto.getName() + "' avec le sujet '" + dto.getSubject() + "' existe déjà.");
        }

        User owner = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + teacherEmail));

        Category category = Category.builder()
                .name(dto.getName().trim())
                .subject(dto.getSubject().trim())
                .status(CategoryStatus.ACTIVE)
                .owner(owner)
                .build();

        Category savedCategory = categoryRepository.save(category);
        followCategory(savedCategory.getId(), teacherEmail);
        return convertToDTO(savedCategory);
    }

    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CreateCategoryDTO dto, String requesterEmail) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée avec l'id : " + id));

        if (category.getOwner() == null || !category.getOwner().getEmail().equals(requesterEmail)) {
            throw new SecurityException("Seul le propriétaire de la catégorie peut la modifier.");
        }

        category.setName(dto.getName().trim());
        category.setSubject(dto.getSubject().trim());

        if (dto.getTeacher() != null && !dto.getTeacher().isEmpty()) {
            User newOwner = userRepository.findByEmail(dto.getTeacher())
                    .orElseThrow(() -> new IllegalArgumentException("Nouvel owner introuvable : " + dto.getTeacher()));
            category.setOwner(newOwner);
        }

        return convertToDTO(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllActiveCategoriesForTeacher(String teacherEmail) {
        List<Category> allActive = categoryRepository.findByStatus(CategoryStatus.ACTIVE);
        User teacher = userRepository.findByEmail(teacherEmail).orElse(null);
        List<Category> ownedByMe = teacher != null ? categoryRepository.findByOwner(teacher) : List.of();

        return java.util.stream.Stream.concat(allActive.stream(), ownedByMe.stream())
                .distinct()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getActiveCategoriesByTeacher(String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + teacherEmail));
        return categoryRepository.findByStatusAndOwner(CategoryStatus.ACTIVE, teacher).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllActiveCategories() {
        return categoryRepository.findByStatus(CategoryStatus.ACTIVE).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<CategoryResponseDTO> getCategoryById(Long id) {
        return categoryRepository.findById(id).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Optional<CategoryResponseDTO> getCategoryByName(String name) {
        return categoryRepository.findByNameIgnoreCase(name).map(this::convertToDTO);
    }

    @Transactional
    public CategoryResponseDTO archiveCategory(Long id, String requesterEmail) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));
        if (category.getOwner() == null || !category.getOwner().getEmail().equals(requesterEmail)) {
            throw new SecurityException("Seul le propriétaire peut archiver cette catégorie.");
        }
        category.setStatus(CategoryStatus.INACTIVE);
        return convertToDTO(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponseDTO unarchiveCategory(Long id, String requesterEmail) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));
        if (category.getOwner() == null || !category.getOwner().getEmail().equals(requesterEmail)) {
            throw new SecurityException("Seul le propriétaire peut désarchiver cette catégorie.");
        }
        category.setStatus(CategoryStatus.ACTIVE);
        return convertToDTO(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id, String requesterEmail) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La catégorie n'existe pas"));
        if (category.getOwner() == null || !category.getOwner().getEmail().equals(requesterEmail)) {
            throw new SecurityException("Seul le propriétaire peut supprimer cette catégorie.");
        }
        if (category.getStatus() != CategoryStatus.INACTIVE) {
            throw new IllegalStateException("Seule une catégorie archivée peut être supprimée définitivement.");
        }
        categoryRepository.deleteById(id);
    }

    private CategoryResponseDTO convertToDTO(Category category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .subject(category.getSubject())
                .status(category.getStatus() != null ? category.getStatus().name().toLowerCase() : null)
                .owner(category.getOwner() != null ? category.getOwner().getEmail() : null)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
