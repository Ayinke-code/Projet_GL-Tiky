package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.RequestManagerDTO;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.model.dto.IssueDTO;
import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.RequestManagerRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RequestManagerRepository requestManagerRepository;

    @InjectMocks
    private IssueService issueService;

    private Issue testIssue;
    private Category testCategory;
    private User testUser;
    private IssueDTO issueDTO;

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

        issueDTO = new IssueDTO();
        issueDTO.setTitre("Test Issue");
        issueDTO.setDescription("Test Description");
        issueDTO.setCategoryId(1L);
    }

    @Test
    void testCreateIssue() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(issueRepository.save(any(Issue.class))).thenReturn(testIssue);

        Issue createdIssue = issueService.createIssue(issueDTO, testUser);

        assertNotNull(createdIssue);
        assertEquals("Test Issue", createdIssue.getTitle());
        assertEquals(IssueStatus.OUVERT, createdIssue.getStatus());
        verify(categoryRepository).findById(1L);
        verify(issueRepository).save(any(Issue.class));
    }

    @Test
    void testCreateIssueWithInvalidCategory() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        issueDTO.setCategoryId(999L);
        assertThrows(IllegalArgumentException.class, () -> issueService.createIssue(issueDTO, testUser));
    }

    @Test
    void testGetIssueById() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(testIssue));

        Issue foundIssue = issueService.getIssueById(1L);

        assertNotNull(foundIssue);
        assertEquals("Test Issue", foundIssue.getTitle());
        verify(issueRepository).findById(1L);
    }

    @Test
    void testGetIssueByIdNotFound() {
        when(issueRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> issueService.getIssueById(999L));
    }

    @Test
    void testGetAllIssues() {
        List<Issue> issues = Arrays.asList(testIssue);
        when(issueRepository.findAll()).thenReturn(issues);

        List<Issue> foundIssues = issueService.getAllIssues();

        assertNotNull(foundIssues);
        assertEquals(1, foundIssues.size());
        verify(issueRepository).findAll();
    }

    @Test
    void testUpdateIssueStatus() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(testIssue));
        when(issueRepository.save(any(Issue.class))).thenReturn(testIssue);

        Issue updatedIssue = issueService.updateIssueStatus(1L, "COMPLET");

        assertNotNull(updatedIssue);
        verify(issueRepository).findById(1L);
        verify(issueRepository).save(any(Issue.class));
    }

    @Test
    void testDeleteIssue() {
        when(issueRepository.existsById(1L)).thenReturn(true);

        issueService.deleteIssue(1L);

        verify(issueRepository).deleteById(1L);
    }

    @Test
    void testDeleteNonExistentIssue() {
        when(issueRepository.existsById(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> issueService.deleteIssue(999L));
    }

    @Test
    void assignManagerToIssue_Success() {
        // Précondition
        testUser.setRoles(Set.of(new Role("TEACHER")));

        // Setup de l'Issue
        User categoryOwner = new User();
        categoryOwner.setId(99L);

        Category category = new Category();
        category.setOwner(categoryOwner);
        testIssue.setCategory(category);

        // Setup de la RequestManager
        RequestManager existingRequest = new RequestManager();
        existingRequest.setIssue(testIssue);


        // Mocks
        when(issueRepository.findById(1L)).thenReturn(Optional.of(testIssue));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(requestManagerRepository.existsByIssueAndStatusAndTargetTeacherIsNotNull(any(), any()))
                .thenReturn(false);

        when(requestManagerRepository.findFirstByIssueOrderByCreatedAtDesc(testIssue))
                .thenReturn(Optional.of(existingRequest));

        // On simule la sauvegarde et le retour de l'objet
        when(requestManagerRepository.save(any(RequestManager.class))).thenAnswer(i -> i.getArgument(0));

        // Si 'toDto' utilise un mapper injecté (ex: RequestManagerMapper), décommente la ligne suivante :
        // when(requestManagerMapper.toDto(any())).thenReturn(new RequestManagerDTO());

        // action
        RequestManagerDTO result = issueService.assignManagerToIssue(1L, 2L);

        // assertion
        assertNotNull(result);

        assertEquals(RequestManagerStatus.PENDING, existingRequest.getStatus());
        assertEquals(testUser, existingRequest.getTargetTeacher());

        // Vérification des appels
        verify(issueRepository).findById(1L);
        verify(userRepository).findById(2L);
        verify(requestManagerRepository).save(any(RequestManager.class));
    }

    @Test
    void assignManagerToIssue_IssueNotFound_ThrowsException() {
        // précondtion
        testUser.setRoles(Set.of(new Role("TEACHER")));

        // Mock
        when(issueRepository.findById(1L)).thenReturn(Optional.empty());

        // Action et Assertion
        assertThrows(EntityNotFoundException.class, () ->
                issueService.assignManagerToIssue(1L, 2L)
        );
    }

    @Test
    void assignManagerToIssue_PendingRequest_ThrowsException() {
        // précondition
        testUser.setRoles(Set.of(new Role("TEACHER")));

        // Arrange
        when(issueRepository.findById(1L)).thenReturn(Optional.of(testIssue));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(requestManagerRepository.existsByIssueAndStatusAndTargetTeacherIsNotNull(testIssue, RequestManagerStatus.PENDING))
                .thenReturn(true);

        // Action et Assertion
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                issueService.assignManagerToIssue(1L, 2L)
        );
        assertEquals("Une demande de manager est déjà en cours pour ce ticket.", exception.getMessage());
    }

    @Test
    void assignManagerToIssue_NotATeacher_ThrowsException() {
        // précondition
        testUser.setRoles(Set.of(new Role("STUDENT")));

        // Mock
        when(issueRepository.findById(1L)).thenReturn(Optional.of(testIssue));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                issueService.assignManagerToIssue(1L, 2L)
        );
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
