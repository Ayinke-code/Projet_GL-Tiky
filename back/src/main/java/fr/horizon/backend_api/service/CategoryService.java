package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.*;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.RoleEnum;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.CategoryRequestRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryRequestRepository categoryRequestRepository;
    private final IssueRepository issueRepository;

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
     * Retire un follower d'une catégorie.
     */
    @Transactional
    public void deleteFollowerById(Long idCategory, Long idUser) {

        User user = userRepository.findById(idUser)
                .orElseThrow(() -> new RuntimeException("User not found " + idUser));

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
                .orElseThrow(() -> new RuntimeException("User not found " + idUser));

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
     * Convertit l'entité Category → DTO.
     */
    private CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .subject(category.getSubject())
                .status(category.getStatus())
                .ownerName(category.getOwner() != null ? category.getOwner().getName() : null)
                .ownerId(category.getOwner() != null ? category.getOwner().getId() : null)
                .build();
    }

    /**
     * Convertit l'entité User → DTO comprenant: id, name, roles.
     */
    private UserRefDto toUserRefDto(User user) {
        return UserRefDto.builder()
                .id(user.getId())
                .name(user.getName())
                .roles(
                        user.getRoles()
                                .stream()
                                .map(Role::getName)
                                .collect(Collectors.toSet())
                )
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
                                .map(this::toUserRefDto)
                                .toList()
                )
                .build();
    }
}
