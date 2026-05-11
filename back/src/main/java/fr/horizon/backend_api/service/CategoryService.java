package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.CategoryFollower;
import fr.horizon.backend_api.model.dto.CategoryResponseDTO;
import fr.horizon.backend_api.model.dto.CreateCategoryDTO;
import fr.horizon.backend_api.exception.CategoryAlreadyExistsException;
import fr.horizon.backend_api.repository.CategoryFollowerRepository;
import fr.horizon.backend_api.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service pour gérer les opérations métier des catégories.
 * Implémente la logique de création, mise à jour, suppression et récupération
 * des catégories.
 * 
 * @author Tiky Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    /**
     * Repository pour l'accès aux données des catégories.
     */
    private final CategoryRepository categoryRepository;
    private final CategoryFollowerRepository followerRepository;
    private final fr.horizon.backend_api.repository.UserRepository userRepository;

    @Transactional
    public void followCategory(Long categoryId, String teacherId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));

        if (!followerRepository.existsByTeacherIdAndCategoryId(teacherId, categoryId)) {
            // Si c'est le proprio, on accepte d'office. Sinon, c'est PENDING.
            String status = String.valueOf(category.getCategoryOwner()).equals(teacherId) ? "ACCEPTED" : "PENDING";
            
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

    public List<Long> getFollowedCategoryIds(String teacherId) {
        return followerRepository.findByTeacherId(teacherId).stream()
                .map(CategoryFollower::getCategoryId)
                .collect(Collectors.toList());
    }

    /**
     * Crée une nouvelle catégorie pour un enseignant.
     * 
     * Règles métier :
     * - Vérifier l'unicité du nom (insensible à la casse)
     * - Créer la catégorie avec le statut "active"
     * - Assigner l'enseignant créateur comme Category Owner
     * - Ajouter l'enseignant comme follower (par défaut)
     * 
     * @param dto       Les données de création de la catégorie
     * @param teacherId L'identifiant de l'enseignant créateur
     * @return La catégorie créée convertie en DTO
     * @throws CategoryAlreadyExistsException Si une catégorie avec le même nom
     *                                        existe déjà
     */

    @Transactional
    public CategoryResponseDTO createCategory(CreateCategoryDTO dto, String teacherId) {
        // 1. Validation de base
        if (dto.getName() == null || dto.getName().trim().isEmpty() ||
                dto.getSubject() == null || dto.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom et le sujet de la catégorie sont requis");
        }

        // 2. Vérification métier : Unicité sur le couple (Name + Subject)
        if (categoryRepository.existsByNameIgnoreCaseAndSubjectIgnoreCase(dto.getName().trim(),
                dto.getSubject().trim())) {
            throw new CategoryAlreadyExistsException(
                    "La catégorie '" + dto.getName() + "' avec le sujet '" + dto.getSubject() + "' existe déjà.");
        }

        // 3. Création de l'entité
        String owner = (dto.getTeacher() != null && !dto.getTeacher().isEmpty()) ? dto.getTeacher() : teacherId;
        
        Category category = Category.builder()
                .name(dto.getName().trim())
                .subject(dto.getSubject().trim())
                .status("active")
                .categoryOwner(owner)
                .build();

        Category savedCategory = categoryRepository.save(category);
        
        // Auto-follow pour le propriétaire
        followCategory(savedCategory.getId(), owner);

        // Ajouter les followers initiaux si spécifiés
        if (dto.getInitialFollowers() != null) {
            for (String fId : dto.getInitialFollowers()) {
                addFollower(savedCategory.getId(), fId);
            }
        }
        
        return convertToDTO(savedCategory);
    }
 
    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CreateCategoryDTO dto, String requesterId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée avec l'id : " + id));
 
        // Vérification des droits : seul le propriétaire peut modifier
        if (!category.getCategoryOwner().equals(requesterId)) {
            throw new SecurityException("Seul le propriétaire de la catégorie peut la modifier.");
        }
 
        category.setName(dto.getName().trim());
        category.setSubject(dto.getSubject().trim());
        
        // Transfert de propriété si spécifié dans le DTO (en utilisant le champ teacher si présent)
        if (dto.getTeacher() != null && !dto.getTeacher().isEmpty()) {
            category.setCategoryOwner(dto.getTeacher());
        }
 
        Category saved = categoryRepository.save(category);
        return convertToDTO(saved);
    }
 
    /**
     * Récupère toutes les catégories actives du système + les catégories possédées par le prof.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllActiveCategoriesForTeacher(String teacherId) {
        // On récupère tout ce qui est actif
        List<Category> allActive = categoryRepository.findByStatus("active");
        
        // On récupère tout ce que possède le prof (même archivé)
        List<Category> ownedByMe = categoryRepository.findByCategoryOwner(teacherId);
        
        // On merge et on convertit en DTO (en évitant les doublons)
        return java.util.stream.Stream.concat(allActive.stream(), ownedByMe.stream())
                .distinct()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
 
    /**
     * Récupère toutes les catégories d'un enseignant.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getCategoriesByTeacher(String teacherId) {
        return categoryRepository.findByCategoryOwner(teacherId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les catégories actives d'un enseignant.
     * 
     * @param teacherId L'identifiant de l'enseignant
     * @return Liste des catégories actives converties en DTOs
     */
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getActiveCategoriesByTeacher(String teacherId) {
        return categoryRepository.findByStatusAndCategoryOwner("active", teacherId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les catégories actives.
     * 
     * @return Liste de toutes les catégories actives converties en DTOs
     */
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllActiveCategories() {
        return categoryRepository.findByStatus("active").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère une catégorie par son identifiant.
     * 
     * @param id L'identifiant de la catégorie
     * @return La catégorie convertie en DTO, ou un Optional vide si non trouvée
     */
    @Transactional(readOnly = true)
    public Optional<CategoryResponseDTO> getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(this::convertToDTO);
    }

    /**
     * Récupère une catégorie par son nom (insensible à la casse).
     * 
     * @param name Le nom de la catégorie
     * @return La catégorie convertie en DTO, ou un Optional vide si non trouvée
     */
    @Transactional(readOnly = true)
    public Optional<CategoryResponseDTO> getCategoryByName(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .map(this::convertToDTO);
    }

    @Transactional
    public CategoryResponseDTO archiveCategory(Long id, String requesterId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));
        
        if (!category.getCategoryOwner().equals(requesterId)) {
            throw new SecurityException("Seul le propriétaire peut archiver cette catégorie.");
        }
        
        category.setStatus("archived");
        return convertToDTO(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponseDTO unarchiveCategory(Long id, String requesterId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));
        
        if (!category.getCategoryOwner().equals(requesterId)) {
            throw new SecurityException("Seul le propriétaire peut désarchiver cette catégorie.");
        }
        
        category.setStatus("active");
        return convertToDTO(categoryRepository.save(category));
    }

    /**
     * Supprime une catégorie.
     */
    @Transactional
    public void deleteCategory(Long id, String requesterId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La catégorie n'existe pas"));
        
        if (!category.getCategoryOwner().equals(requesterId)) {
            throw new SecurityException("Seul le propriétaire peut supprimer cette catégorie.");
        }

        if (!"archived".equals(category.getStatus())) {
            throw new IllegalStateException("Seule une catégorie archivée peut être supprimée définitivement.");
        }

        categoryRepository.deleteById(id);
    }

    public List<User> getFollowers(Long categoryId) {
        List<String> teacherIds = followerRepository.findByCategoryId(categoryId).stream()
                .filter(f -> "ACCEPTED".equals(f.getStatus())) // Uniquement les acceptés
                .map(CategoryFollower::getTeacherId)
                .collect(Collectors.toList());
        
        if (teacherIds.isEmpty()) return java.util.Collections.emptyList();

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
                    .status("ACCEPTED") // Ajout par le proprio = d'office accepté
                    .build());
        }
    }

    @Transactional
    public void removeFollower(Long categoryId, String teacherId) {
        followerRepository.deleteByTeacherIdAndCategoryId(teacherId, categoryId);
    }

    public List<User> getAvailableTeachers(Long categoryId) {
        return userRepository.findTeachersNotFollowingCategory(categoryId);
    }

    @Transactional
    public void approveFollower(Long categoryId, String teacherId, String requesterId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));

        if (!category.getCategoryOwner().equals(requesterId)) {
            throw new SecurityException("Seul le propriétaire peut approuver un follower.");
        }

        CategoryFollower follower = followerRepository.findByTeacherIdAndCategoryId(teacherId, categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'abonnement non trouvée"));

        follower.setStatus("ACCEPTED");
        followerRepository.save(follower);
    }

    @Transactional
    public void rejectFollower(Long categoryId, String teacherId, String requesterId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));

        if (!category.getCategoryOwner().equals(requesterId)) {
            throw new SecurityException("Seul le propriétaire peut rejeter un follower.");
        }

        followerRepository.deleteByTeacherIdAndCategoryId(teacherId, categoryId);
    }

    public List<CategoryFollower> getPendingRequests(Long categoryId) {
        return followerRepository.findByCategoryId(categoryId).stream()
                .filter(f -> "PENDING".equals(f.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Convertit une entité Category en DTO CategoryResponseDTO.
     * 
     * @param category L'entité à convertir
     * @return Le DTO correspondant
     */
    private CategoryResponseDTO convertToDTO(Category category) {
        List<User> followers = getFollowers(category.getId());
        
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .subject(category.getSubject())
                .status(category.getStatus())
                .owner(category.getCategoryOwner())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .followers(new java.util.HashSet<>(followers))
                .build();
    }
}
