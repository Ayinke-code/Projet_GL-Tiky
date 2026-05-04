package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.IssueDTO;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class IssueService {

    private final IssueRepository issueRepository;
    private final CategoryRepository categoryRepository;

    public Issue createIssue(IssueDTO dto, User creator) {
        validateIssueInput(dto);

        Category category = categoryRepository.findById(dto.getCategorieId())
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée"));

        Issue issue = Issue.builder()
                .titre(dto.getTitre())
                .description(dto.getDescription())
                .categorie(category)
                .createur(creator)
                .statut("en attente de traitement")
                .estPrive(false)
                .build();

        return issueRepository.save(issue);
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
        return issueRepository.findByStatut(statut);
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
}
