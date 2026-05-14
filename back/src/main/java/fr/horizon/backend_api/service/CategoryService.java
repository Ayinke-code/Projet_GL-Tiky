package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.CategoryFollower;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.CategoryCreateDto;
import fr.horizon.backend_api.model.dto.CategoryResponseDTO;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.repository.CategoryFollowerRepository;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.CategoryRequestRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryFollowerRepository followerRepository;
    private final UserRepository userRepository;
    private final CategoryRequestRepository categoryRequestRepository;

    @Transactional
    public void followCategory(Long categoryId, String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + teacherEmail));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));

        if (!followerRepository.existsByUserAndCategory(teacher, category)) {
            followerRepository.save(CategoryFollower.builder()
                    .user(teacher)
                    .category(category)
                    .build());
        }
    }

    @Transactional
    public void unfollowCategory(Long categoryId, String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + teacherEmail));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));

        followerRepository.deleteByUserAndCategory(teacher, category);
    }

    public List<Long> getFollowedCategoryIds(String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + teacherEmail));

        return followerRepository.findByUser(teacher).stream()
                .map(f -> f.getCategory().getId())
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponseDTO createCategory(CategoryCreateDto dto, String teacherEmail) {
        if (dto.getName() == null || dto.getName().trim().isEmpty() || dto.getSubject() == null || dto.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom et le sujet de la catégorie sont requis");
        }

        if (categoryRepository.existsByNameIgnoreCase(dto.getName().trim())) {
            throw new IllegalArgumentException("La catégorie existe déjà");
        }

        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + teacherEmail));

        Category category = Category.builder()
                .name(dto.getName().trim())
                .subject(dto.getSubject().trim())
                .status(CategoryStatus.ACTIVE)
                .categoryOwner(teacher)
                .build();

        Category saved = categoryRepository.save(category);

        followCategory(saved.getId(), teacherEmail);

        return convertToDTO(saved);
    }

    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryCreateDto dto, String requesterEmail) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée avec l'id : " + id));

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + requesterEmail));

        if (category.getCategoryOwner() == null || !category.getCategoryOwner().getId().equals(requester.getId())) {
            throw new SecurityException("Seul le propriétaire de la catégorie peut la modifier.");
        }

        category.setName(dto.getName().trim());
        category.setSubject(dto.getSubject().trim());

        Category saved = categoryRepository.save(category);
        return convertToDTO(saved);
    }

    public List<CategoryResponseDTO> getAllActiveCategoriesForTeacher(String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + teacherEmail));

        List<Category> allActive = categoryRepository.findByStatus(CategoryStatus.ACTIVE);
        List<Category> ownedByMe = categoryRepository.findByCategoryOwner(teacher);

        return java.util.stream.Stream.concat(allActive.stream(), ownedByMe.stream())
                .distinct()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CategoryResponseDTO> getCategoriesByTeacher(String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + teacherEmail));

        return categoryRepository.findByCategoryOwner(teacher).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CategoryResponseDTO> getActiveCategoriesByTeacher(String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + teacherEmail));

        return categoryRepository.findByStatusAndCategoryOwner(CategoryStatus.ACTIVE, teacher).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CategoryResponseDTO> getAllActiveCategories() {
        return categoryRepository.findByStatus(CategoryStatus.ACTIVE).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<CategoryResponseDTO> getCategoryById(Long id) {
        return categoryRepository.findById(id).map(this::convertToDTO);
    }

    public Optional<CategoryResponseDTO> getCategoryByName(String name) {
        return categoryRepository.findByNameIgnoreCase(name).map(this::convertToDTO);
    }

    @Transactional
    public CategoryResponseDTO archiveCategory(Long id, String requesterEmail) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + requesterEmail));

        if (category.getCategoryOwner() == null || !category.getCategoryOwner().getId().equals(requester.getId())) {
            throw new SecurityException("Seul le propriétaire peut archiver cette catégorie.");
        }

        category.setStatus(CategoryStatus.INACTIVE);
        return convertToDTO(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponseDTO unarchiveCategory(Long id, String requesterEmail) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + requesterEmail));

        if (category.getCategoryOwner() == null || !category.getCategoryOwner().getId().equals(requester.getId())) {
            throw new SecurityException("Seul le propriétaire peut désarchiver cette catégorie.");
        }

        category.setStatus(CategoryStatus.ACTIVE);
        return convertToDTO(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id, String requesterEmail) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La catégorie n'existe pas"));

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + requesterEmail));

        if (category.getCategoryOwner() == null || !category.getCategoryOwner().getId().equals(requester.getId())) {
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
                .status(category.getStatus() == CategoryStatus.ACTIVE ? "ACTIVE" : "ARCHIVED")
                .owner(category.getCategoryOwner() != null ? category.getCategoryOwner().getEmail() : null)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}