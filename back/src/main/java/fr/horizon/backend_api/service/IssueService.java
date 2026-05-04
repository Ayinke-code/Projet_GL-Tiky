package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.IssueDTO;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Transactional
public class IssueService {

    private final IssueRepository issueRepository;
    private final CategoryRepository categoryRepository;
    private final String uploadDir = "uploads/issues/";

    public IssueService(IssueRepository issueRepository,
                        CategoryRepository categoryRepository) {
        this.issueRepository = issueRepository;
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
                .statut("en attente de traitement")
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

    private void saveDocuments(Issue issue, java.util.List<MultipartFile> documents) {
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
}
