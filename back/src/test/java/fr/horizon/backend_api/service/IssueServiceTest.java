package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.IssueDto;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.RequestManagerRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private RequestManagerRepository requestManagerRepository;

    @InjectMocks
    private IssueService issueService;

    private Issue testIssue;
    private Category testCategory;
    private User testUser;
    private IssueDto issueDTO;

    private Role teacherRole;
    private Role studentRole;

    @BeforeEach
    void setUp() {
        teacherRole = new Role("TEACHER");
        studentRole = new Role("STUDENT");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Test Category");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.fr");

        testIssue = new Issue();
        testIssue.setId(1L);
        testIssue.setTitle("Test Issue");
        testIssue.setDescription("Test Description");
        testIssue.setStatus(IssueStatus.OUVERT);
        testIssue.setCategory(testCategory);
        testIssue.setCreatedBy(testUser);

        issueDTO = new IssueDto();
        issueDTO.setTitle("Test Issue");
        issueDTO.setDescription("Test Description");
        issueDTO.setCategoryId(1L);
    }


    @Test
    void markIssueAsIncomplete_Success() {
        // Arrange
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setStatus(IssueStatus.OUVERT);

        RequestManager requestManager = new RequestManager();
        requestManager.setStatus(RequestManagerStatus.PENDING);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(requestManagerRepository.findByIssue_Id(issueId)).thenReturn(Optional.of(requestManager));

        // Act
        issueService.markIssueAsIncomplete(issueId);

        // Assert
        assertEquals(IssueStatus.INCOMPLET, issue.getStatus());
        assertEquals(RequestManagerStatus.DENIED, requestManager.getStatus());

        verify(issueRepository).save(issue);
        verify(requestManagerRepository).save(requestManager);
    }

    @Test
    void markIssueAsIncomplete_Already_Close() {
        // Arrange
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setStatus(IssueStatus.TERMINE); // Déjà résolue

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            issueService.markIssueAsIncomplete(issueId);
        });

        // On vérifie qu'aucun save n'a été appelé car l'exception est jetée avant
        verify(issueRepository, never()).save(any());
        verify(requestManagerRepository, never()).save(any());
    }

    @Test
    void markIssueAsIncomplete_IssueNotFound() {
        // Arrange
        Long issueId = 1L;
        when(issueRepository.findById(issueId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            issueService.markIssueAsIncomplete(issueId);
        });

        verify(requestManagerRepository, never()).findByIssue_Id(anyLong());
    }



}
