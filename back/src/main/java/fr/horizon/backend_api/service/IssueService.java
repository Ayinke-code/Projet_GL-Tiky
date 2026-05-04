package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.IssueStatus;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.IssueDTO;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class IssueService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final String uploadDir = "uploads/issues/";

    public IssueService(IssueRepository issueRepository,
                        CategoryRepository categoryRepository, UserRepository userRepository) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    public Issue createIssue(IssueDTO dto, User creator) {
        validateIssueInput(dto);

        Category category = categoryRepository.findById(dto.getCategorieId())
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));

        if (!category.getEstValide()) {
            throw new IllegalArgumentException("Catégorie inactive ou non validée");
        }

        Issue issue = Issue.builder()
                .titre(dto.getTitre())
                .description(dto.getDescription())
                .categorie(category)
                .createur(creator)
                .statut(IssueStatus.INCOMPLETE)
                .estPrive(false)
                .build();

        Issue savedIssue = issueRepository.save(issue);

        if (dto.getDocuments() != null && !dto.getDocuments().isEmpty()) {
            saveDocuments(savedIssue, dto.getDocuments());
        }

        return savedIssue;
    }

    public Issue getIssueById(Long id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Issue non trouvée"));
    }

    public List<Issue> getIssuesByCategory(Long categorieId) {
        return issueRepository.findByCategorie_IdCat(categorieId);
    }

    public List<Issue> getIssuesByCreator(Long creatorId) {
        return issueRepository.findByCreateur_Id(creatorId);
    }

    public List<Issue> getIssuesByStatus(String statut) {
        try {
            IssueStatus parsedStatus = IssueStatus.valueOf(statut.toUpperCase());
            return issueRepository.findByStatut(parsedStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Statut invalide");
        }
    }

    public void deleteIssue(Long id) {
        if (!issueRepository.existsById(id)) {
            throw new IllegalArgumentException("Issue non trouvée");
        }
        issueRepository.deleteById(id);
    }

    private void validateIssueInput(IssueDTO dto) {
        if (dto.getTitre() == null || dto.getTitre().isBlank()) {
            throw new IllegalArgumentException("Le titre est obligatoire");
        }
        if (dto.getDescription() == null || dto.getDescription().isBlank()) {
            throw new IllegalArgumentException("La description est obligatoire");
        }
        if (dto.getCategorieId() == null) {
            throw new IllegalArgumentException("La catégorie est obligatoire");
        }
    }

    private void saveDocuments(Issue issue, List<MultipartFile> documents) {
        try {
            Path uploadPath = Paths.get(uploadDir + issue.getIdTick());
            Files.createDirectories(uploadPath);

            for (MultipartFile file : documents) {
                if (!file.isEmpty()) {
                    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    Path filePath = uploadPath.resolve(fileName);
                    Files.copy(file.getInputStream(), filePath);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du téléchargement des fichiers", e);
        }
    }

    @Transactional
    private void assignManagerToIssue(Long issueId, Long userId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket non trouvé"));

        User teacher = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        if (!teacher.isTeacher()) {
            throw new IllegalArgumentException("L'utilisateur visé n'est pas un teacher");
        }

        // issue.setManager(teacher);
        issue.setStatut(IssueStatus.VALIDE);
    }
}
