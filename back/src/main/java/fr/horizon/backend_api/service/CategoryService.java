package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.dto.CategoryResponseDTO;
import fr.horizon.backend_api.model.dto.CreateCategoryDTO;
import fr.horizon.backend_api.exception.CategoryAlreadyExistsException;
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
        Category category = Category.builder()
                .name(dto.getName().trim())
                .subject(dto.getSubject().trim()) // Assure-toi que ce champ existe dans ton Entity
                .status("active")
                .categoryOwner(teacherId)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return convertToDTO(savedCategory);
    }

    public CategoryResponseDTO updateCategory(Long id, CreateCategoryDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée avec l'id : " + id));

        category.setName(dto.getName());
        category.setSubject(dto.getSubject());
        // ... autres champs si nécessaire

        Category saved = categoryRepository.save(category);
        return convertToDTO(saved);
    }

    /**
     * Récupère toutes les catégories d'un enseignant.
     * 
     * @param teacherId L'identifiant de l'enseignant
     * @return Liste des catégories de l'enseignant converties en DTOs
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

    /**
     * Supprime une catégorie.
     * 
     * @param id L'identifiant de la catégorie
     * @throws IllegalArgumentException Si la catégorie n'existe pas
     */
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("La catégorie avec l'ID " + id + " n'existe pas");
        }
        categoryRepository.deleteById(id);
    }

    /**
     * Convertit une entité Category en DTO CategoryResponseDTO.
     * 
     * @param category L'entité à convertir
     * @return Le DTO correspondant
     */
    private CategoryResponseDTO convertToDTO(Category category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .subject(category.getSubject()) // Ajoute ce mapping
                .status(category.getStatus())
                .owner(category.getCategoryOwner())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
