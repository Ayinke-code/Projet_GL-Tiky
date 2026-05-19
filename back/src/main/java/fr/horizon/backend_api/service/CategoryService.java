package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.CategoryDto;
import fr.horizon.backend_api.model.dto.CategoryCreateDto;
import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryRequestRepository categoryRequestRepository;
    private final IssueRepository issueRepository;
    private final RoleRepository roleRepository;

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
    @Transactional
    public void deleteById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        List<CategoryRequest> requests = categoryRequestRepository.findAllByCategory(category);
        List<Issue> issues = issueRepository.findByCategoryId(category.getId());

        issueRepository.deleteAll(issues);
        categoryRequestRepository.deleteAll(requests);

        categoryRepository.delete(category);
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
