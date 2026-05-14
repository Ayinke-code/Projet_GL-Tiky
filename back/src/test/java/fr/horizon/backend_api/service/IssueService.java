package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.RequestManagerDto;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.model.dto.IssueDto;
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
        issueDTO.setTitre("Test Issue");
        issueDTO.setDescription("Test Description");
        issueDTO.setCategorieId(1L);
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

        // action
        RequestManagerDto result = issueService.assignManagerToIssue(1L, 2L);

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
}
