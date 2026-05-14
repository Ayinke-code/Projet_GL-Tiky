//package fr.horizon.backend_api.service;
//
//import fr.horizon.backend_api.model.Issue;
//import fr.horizon.backend_api.model.Category;
//import fr.horizon.backend_api.model.User;
//import fr.horizon.backend_api.model.enums.IssueStatus;
//import fr.horizon.backend_api.model.dto.IssueDto;
//import fr.horizon.backend_api.repository.IssueRepository;
//import fr.horizon.backend_api.repository.CategoryRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class IssueServiceTest {
//
//    @Mock
//    private IssueRepository issueRepository;
//
//    @Mock
//    private CategoryRepository categoryRepository;
//
//    @InjectMocks
//    private IssueService issueService;
//
//    private Issue testIssue;
//    private Category testCategory;
//    private User testUser;
//    private IssueDto issueDto;
//
//    @BeforeEach
//    void setUp() {
//        testCategory = new Category();
//        testCategory.setId(1L);
//        testCategory.setName("Test Category");
//
//        testUser = new User();
//        testUser.setId(1L);
//        testUser.setEmail("test@test.fr");
//
//        testIssue = new Issue();
//        testIssue.setIdTick(1L);
//        testIssue.setTitle("Test Issue");
//        testIssue.setDescription("Test Description");
//        testIssue.setStatus(IssueStatus.OUVERT);
//        testIssue.setCategorie(testCategory);
//        testIssue.setOwner(testUser);
//
//        issueDto = new IssueDto();
//        issueDto.setTitle("Test Issue");
//        issueDto.setDescription("Test Description");
//        issueDto.setCategoryId(1L);
//    }
//
//    @Test
//    void testCreateIssue() {
//        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
//        when(issueRepository.save(any(Issue.class))).thenReturn(testIssue);
//
//        Issue createdIssue = issueService.createIssue(issueDto, testUser);
//
//        assertNotNull(createdIssue);
//        assertEquals("Test Issue", createdIssue.getTitle());
//        assertEquals(IssueStatus.OUVERT, createdIssue.getStatus());
//        verify(categoryRepository).findById(1L);
//        verify(issueRepository).save(any(Issue.class));
//    }
//
//    @Test
//    void testCreateIssueWithInvalidCategory() {
//        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
//
//        issueDto.setCategoryId(999L);
//        assertThrows(IllegalArgumentException.class, () -> issueService.createIssue(issueDto, testUser));
//    }
//
//    @Test
//    void testGetIssueById() {
//        when(issueRepository.findById(1L)).thenReturn(Optional.of(testIssue));
//
//        Issue foundIssue = issueService.getIssueById(1L);
//
//        assertNotNull(foundIssue);
//        assertEquals("Test Issue", foundIssue.getTitle());
//        verify(issueRepository).findById(1L);
//    }
//
//    @Test
//    void testGetIssueByIdNotFound() {
//        when(issueRepository.findById(999L)).thenReturn(Optional.empty());
//
//        assertThrows(IllegalArgumentException.class, () -> issueService.getIssueById(999L));
//    }
//
//    @Test
//    void testGetAllIssues() {
//        List<Issue> issues = Arrays.asList(testIssue);
//        when(issueRepository.findAll()).thenReturn(issues);
//
//        List<Issue> foundIssues = issueService.getAllIssues();
//
//        assertNotNull(foundIssues);
//        assertEquals(1, foundIssues.size());
//        verify(issueRepository).findAll();
//    }
//
//    @Test
//    void testUpdateIssueStatus() {
//        when(issueRepository.findById(1L)).thenReturn(Optional.of(testIssue));
//        when(issueRepository.save(any(Issue.class))).thenReturn(testIssue);
//
//        Issue updatedIssue = issueService.updateIssueStatus(1L, "COMPLET");
//
//        assertNotNull(updatedIssue);
//        verify(issueRepository).findById(1L);
//        verify(issueRepository).save(any(Issue.class));
//    }
//
//    @Test
//    void testDeleteIssue() {
//        when(issueRepository.existsById(1L)).thenReturn(true);
//
//        issueService.deleteIssue(1L);
//
//        verify(issueRepository).deleteById(1L);
//    }
//
//    @Test
//    void testDeleteNonExistentIssue() {
//        when(issueRepository.existsById(999L)).thenReturn(false);
//
//        assertThrows(IllegalArgumentException.class, () -> issueService.deleteIssue(999L));
//    }
//}