package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.CategoryDto;
import fr.horizon.backend_api.model.dto.CategoryCreateDto;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * Récupère toutes les catégories.
     */
    public List<CategoryDto> findAll() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Crée une nouvelle catégorie.
     */
    public CategoryDto createCategory(CategoryCreateDto dto) {

        User owner = userRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Category category = Category.builder()
                .name(dto.getName())
                .subject(dto.getSubject())
                .status(CategoryStatus.ACTIVE)
                .owner(owner)
                .build();

        categoryRepository.save(category);

        return toDto(category);
    }

    /**
     * Supprime une catégorie par ID.
     */
    public void deleteById(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new CategoryNotFoundException(id);
        }
        categoryRepository.deleteById(id);
    }

    /**
     * Convertit entité → DTO.
     */
    private CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .subject(category.getSubject())
                .status(category.getStatus())
                .ownerName(category.getOwner() != null ? category.getOwner().getName() : null)
                .build();
    }
}
