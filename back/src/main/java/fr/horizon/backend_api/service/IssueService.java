package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.model.dto.IssueDTO;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Transactional
public class IssueService {

    private final IssueRepository issueRepository;
    private final CategoryRepository categoryRepository;
    private static final Logger logger = LoggerFactory.getLogger(IssueService.class);

    public Issue createIssue(IssueDTO dto, User creator) {
        validateIssueInput(dto);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Issue issue = Issue.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(category)
                .createdBy(creator)
                .status(IssueStatus.OPEN)
                .isPrivate(false)
                .build();

        return issueRepository.save(issue);
    }

    public Issue getIssueById(Long id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found"));
    }

    public List<Issue> getAllIssues() {
        return issueRepository.findAll();
    }

    public List<Issue> getIssuesByCategory(Long categoryId) {
        return issueRepository.findByCategoryId(categoryId);
    }

    public List<Issue> getIssuesByCreator(Long creatorId) {
        return issueRepository.findByCreatedById(creatorId);
    }

    public List<Issue> getIssuesByStatus(String statusStr) {
        IssueStatus status = IssueStatus.fromValue(statusStr);
        return issueRepository.findByStatus(status);
    }

    public void deleteIssue(Long id) {
        if (!issueRepository.existsById(id)) {
            throw new IllegalArgumentException("Issue not found");
        }
        issueRepository.deleteById(id);
    }

    public Issue updateIssueStatus(Long id, String newStatus) {
        logger.info("updateIssueStatus called with id={}, newStatus={}", id, newStatus);
        Issue issue = getIssueById(id);
        logger.info("Current status before update: {}", issue.getStatus());
        
        IssueStatus status = IssueStatus.fromValue(newStatus);
        logger.info("Parsed status: {}", status);
        
        issue.setStatus(status);
        Issue saved = issueRepository.save(issue);
        logger.info("Status after save: {}", saved.getStatus());
        
        return saved;
    }

    private void validateIssueInput(IssueDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is mandatory");
        }
        if (dto.getDescription() == null || dto.getDescription().isBlank()) {
            throw new IllegalArgumentException("Description is mandatory");
        }
        if (dto.getCategoryId() == null) {
            throw new IllegalArgumentException("Category is mandatory");
        }
    }
}
