package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.exception.IssueNotFoundException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.IssueCreateDto;
import fr.horizon.backend_api.model.dto.IssueDto;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service de gestion des issues.
 * <p>
 * Fournit les opérations métier liées aux issues :
 * création, récupération, suppression et conversion en DTO.
 * <p>
 * Gère les relations entre Issue, User (owner) et Category,
 * ainsi que les règles de création et de suppression.
 */
@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Retourne la liste de toutes les issues.
     *
     * @return liste de DTO issues
     */
    public List<IssueDto> findAll() {
        return issueRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Retourne l'issue correspondant à l'id.
     *
     * @param id id de l'issue à trouver
     * @return DTO de l'issue
     */
    public IssueDto findById(Long id) {
        return issueRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IssueNotFoundException(id));
    }

    /**
     * Crée une nouvelle issue.
     * <p>
     * L'issue est associée à un utilisateur (owner) via son email
     * et à une catégorie existante.
     *
     * @param dto   données de création de l'issue
     * @param email email de l'utilisateur créateur
     * @return issue créée sous forme de DTO
     * @throws RuntimeException si l'utilisateur n'existe pas
     * @throws CategoryNotFoundException si la catégorie n'existe pas
     */
    @Transactional
    public IssueDto createIssue(IssueCreateDto dto, String email) {

        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(dto.getCategoryId()));

        Issue issue = Issue.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(category)
                .createdBy(owner)
                .build();

        issueRepository.save(issue);

        return toDto(issue);
    }

    /**
     * Supprime une issue par son identifiant.
     *
     * @param id identifiant de l'issue
     * @throws IssueNotFoundException si l'issue n'existe pas
     */
    @Transactional
    public void deleteById(Long id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id));

        issueRepository.delete(issue);
    }

    /**
     * Convertit une entité Issue en DTO.
     *
     * @param issue entité issue
     * @return DTO issue
     */
    public IssueDto toDto(Issue issue) {
        return IssueDto.builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .status(issue.getStatus())
                .isPrivate(issue.isPrivate())
                .rejectType(issue.getRejectType())
                .rejectReason(issue.getRejectReason())
                .rejectComment(issue.getRejectComment())
                // Category
                .categoryId(issue.getCategory().getId())
                .categoryName(issue.getCategory().getName())
                .categoryOwnerId(issue.getCategory().getOwner().getId())
                .categoryOwnerName(issue.getCategory().getOwner().getName())
                // CreatedBy
                .createdId(issue.getCreatedBy().getId())
                .createdName(issue.getCreatedBy().getName())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }
}
