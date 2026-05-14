//package fr.horizon.backend_api.service;
//
//import fr.horizon.backend_api.exception.CategoryNotFoundException;
//import fr.horizon.backend_api.exception.IssueNotFoundException;
//import fr.horizon.backend_api.model.Category;
//import fr.horizon.backend_api.model.Issue;
//import fr.horizon.backend_api.model.User;
//import fr.horizon.backend_api.model.dto.IssueCreateDto;
//import fr.horizon.backend_api.model.dto.IssueDto;
//import fr.horizon.backend_api.model.enums.IssueStatus;
//import fr.horizon.backend_api.repository.CategoryRepository;
//import fr.horizon.backend_api.repository.IssueRepository;
//import fr.horizon.backend_api.repository.UserRepository;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.when;
//import static org.mockito.Mockito.verify;
//import static org.mockito.ArgumentMatchers.any;
//
//@ExtendWith(MockitoExtension.class)
//class IssueServiceTest {
//
//    @Mock
//    private IssueRepository issueRepository;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private CategoryRepository categoryRepository;
//
//    @InjectMocks
//    private IssueService issueService;
//
//    @Test
//    void createIssue_should_save_issue_when_valid() {
//
//        User categoryOwner = new User();
//        categoryOwner.setId(99L);
//        categoryOwner.setName("Teacher");
//
//        IssueCreateDto dto = new IssueCreateDto();
//        dto.setTitle("Bug");
//        dto.setDescription("Bug description");
//        dto.setCategoryId(1L);
//
//        User owner = new User();
//        owner.setId(1L);
//        owner.setEmail("john@test.com");
//        owner.setName("John");
//
//        Category category = new Category();
//        category.setId(1L);
//        category.setName("Software");
//        category.setOwner(categoryOwner);
//
//        when(userRepository.findByEmail("john@test.com"))
//                .thenReturn(Optional.of(owner));
//
//        when(categoryRepository.findById(1L))
//                .thenReturn(Optional.of(category));
//
//        when(issueRepository.save(any(Issue.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        IssueDto result = issueService.createIssue(dto, "john@test.com");
//
//        assertNotNull(result);
//        assertEquals("Bug", result.getTitle());
//        assertEquals("Bug description", result.getDescription());
//        assertEquals(1L, result.getCategoryId());
//        assertEquals("Software", result.getCategoryName());
//        assertEquals(1L, result.getCreatedId());
//        assertEquals("John", result.getCreatedName());
//    }
//
//    @Test
//    void createIssue_should_throw_when_user_not_found() {
//
//        IssueCreateDto dto = new IssueCreateDto();
//        dto.setCategoryId(1L);
//
//        when(userRepository.findByEmail("john@test.com"))
//                .thenReturn(Optional.empty());
//
//        assertThrows(RuntimeException.class,
//                () -> issueService.createIssue(dto, "john@test.com"));
//    }
//
//    @Test
//    void createIssue_should_throw_when_category_not_found() {
//
//        IssueCreateDto dto = new IssueCreateDto();
//        dto.setCategoryId(1L);
//
//        User owner = new User();
//        owner.setId(1L);
//        owner.setEmail("john@test.com");
//
//        when(userRepository.findByEmail("john@test.com"))
//                .thenReturn(Optional.of(owner));
//
//        when(categoryRepository.findById(1L))
//                .thenReturn(Optional.empty());
//
//        assertThrows(CategoryNotFoundException.class,
//                () -> issueService.createIssue(dto, "john@test.com"));
//    }
//
//    @Test
//    void findAll_should_return_list_of_dto() {
//
//        User categoryOwner = new User();
//        categoryOwner.setId(99L);
//        categoryOwner.setName("Teacher");
//
//        User user = new User();
//        user.setId(1L);
//        user.setName("John");
//
//        Category category = new Category();
//        category.setId(1L);
//        category.setName("Software");
//        category.setOwner(categoryOwner);
//
//        Issue issue = Issue.builder()
//                .id(1L)
//                .title("Bug")
//                .description("desc")
//                .category(category)
//                .createdBy(user)
//                .build();
//
//        when(issueRepository.findAll())
//                .thenReturn(List.of(issue));
//
//        List<IssueDto> result = issueService.findAll();
//
//        assertEquals(1, result.size());
//        assertEquals("Bug", result.get(0).getTitle());
//        assertEquals(1L, result.get(0).getCategoryId());
//        assertEquals("Software", result.get(0).getCategoryName());
//    }
//
//    @Test
//    void deleteById_should_delete_issue_when_exists() {
//
//        Issue issue = new Issue();
//        issue.setId(1L);
//
//        when(issueRepository.findById(1L))
//                .thenReturn(Optional.of(issue));
//
//        issueService.deleteById(1L);
//
//        verify(issueRepository).delete(issue);
//    }
//
//    @Test
//    void deleteById_should_throw_when_not_found() {
//
//        when(issueRepository.findById(1L))
//                .thenReturn(Optional.empty());
//
//        assertThrows(IssueNotFoundException.class,
//                () -> issueService.deleteById(1L));
//    }
//
//    @Test
//    void toDto_should_map_issue_correctly() {
//
//        User categoryOwner = new User();
//        categoryOwner.setId(99L);
//        categoryOwner.setName("Teacher");
//
//        User user = new User();
//        user.setId(1L);
//        user.setName("John");
//
//        Category category = new Category();
//        category.setId(1L);
//        category.setName("Software");
//        category.setOwner(categoryOwner);
//
//        Issue issue = Issue.builder()
//                .id(1L)
//                .title("Bug")
//                .description("desc")
//                .status(IssueStatus.OUVERT)
//                .category(category)
//                .createdBy(user)
//                .build();
//
//        IssueDto dto = issueService.toDto(issue);
//
//        assertEquals(1L, dto.getId());
//        assertEquals("Bug", dto.getTitle());
//        assertEquals("desc", dto.getDescription());
//        assertEquals(1L, dto.getCategoryId());
//        assertEquals("Software", dto.getCategoryName());
//        assertEquals(1L, dto.getCreatedId());
//        assertEquals("John", dto.getCreatedName());
//    }
//
//    @Test
//    void findById_should_return_issue() {
//
//        User categoryOwner = new User();
//        categoryOwner.setId(99L);
//        categoryOwner.setName("Teacher");
//
//        Category category = Category.builder()
//                .id(10L)
//                .name("DEV")
//                .build();
//        category.setOwner(categoryOwner);
//
//        User user = User.builder()
//                .id(5L)
//                .name("John")
//                .build();
//
//        Issue issue = Issue.builder()
//                .id(1L)
//                .title("Bug")
//                .description("desc")
//                .category(category)
//                .createdBy(user)
//                .build();
//
//        when(issueRepository.findById(1L))
//                .thenReturn(Optional.of(issue));
//
//        IssueDto result = issueService.findById(1L);
//
//        assertNotNull(result);
//        assertEquals(1L, result.getId());
//        assertEquals("Bug", result.getTitle());
//        assertEquals(10L, result.getCategoryId());
//        assertEquals("DEV", result.getCategoryName());
//        assertEquals(99L, result.getCategoryOwnerId());
//        assertEquals("Teacher", result.getCategoryOwnerName());
//    }
//
//    @Test
//    void findById_should_throw_when_not_found() {
//
//        when(issueRepository.findById(1L))
//                .thenReturn(Optional.empty());
//
//        assertThrows(IssueNotFoundException.class,
//                () -> issueService.findById(1L));
//    }
//
//    @Test
//    void updateStatus_should_update_issue_status() {
//
//        Issue issue = new Issue();
//        issue.setId(1L);
//        issue.setStatus(IssueStatus.OUVERT);
//
//        when(issueRepository.findById(1L))
//                .thenReturn(Optional.of(issue));
//
//        issueService.updateStatus("TERMINE", 1L);
//
//        assertEquals(IssueStatus.TERMINE, issue.getStatus());
//        verify(issueRepository).save(issue);
//    }
//
//    @Test
//    void updateStatus_should_throw_when_invalid_status() {
//
//        assertThrows(IllegalArgumentException.class,
//                () -> issueService.updateStatus("INVALID", 1L));
//    }
//
//    @Test
//    void updatePrivacy_should_toggle_privacy() {
//
//        Issue issue = new Issue();
//        issue.setId(1L);
//        issue.setPrivate(false);
//
//        when(issueRepository.findById(1L))
//                .thenReturn(Optional.of(issue));
//
//        issueService.updatePrivacy(1L);
//
//        assertTrue(issue.isPrivate());
//        verify(issueRepository).save(issue);
//    }
//
//    @Test
//    void updatePrivacy_should_toggle_from_true_to_false() {
//
//        Issue issue = new Issue();
//        issue.setId(1L);
//        issue.setPrivate(true);
//
//        when(issueRepository.findById(1L))
//                .thenReturn(Optional.of(issue));
//
//        issueService.updatePrivacy(1L);
//
//        assertFalse(issue.isPrivate());
//        verify(issueRepository).save(issue);
//    }
//}
//