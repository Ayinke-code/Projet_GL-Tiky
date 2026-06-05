package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.exception.IssueNotFoundException;
import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.IssueCreateDto;
import fr.horizon.backend_api.model.dto.IssueDto;
import fr.horizon.backend_api.model.dto.RequestManagerDto;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import fr.horizon.backend_api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RequestManagerRepository requestManagerRepository;

    @Mock
    private ConversationService conversationService;

    @Mock
    private IssueFollowerService issueFollowerService;

    @Mock
    private IssueFollowerRepository issueFollowerRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private IssueService issueService;

    private final String commentaire = "commentaire";
    private final String raison = "raison";

    @Test
    void createIssue_should_save_with_OUVERT_status_and_create_manager_when_not_pending() {

        User categoryOwner = new User();
        categoryOwner.setId(99L);
        categoryOwner.setName("Teacher");

        IssueCreateDto dto = new IssueCreateDto();
        dto.setTitle("Bug");
        dto.setDescription("Bug description");
        dto.setCategoryId(1L);
        dto.setPendingCase(false);

        User owner = new User();
        owner.setId(1L);
        owner.setEmail("john@test.com");
        owner.setName("John");

        Category category = new Category();
        category.setId(1L);
        category.setOwner(categoryOwner);

        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.of(owner));

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(issueRepository.save(any(Issue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // IMPORTANT
        when(documentService.storeFiles(
                anyList(),
                any(Issue.class),
                any(User.class),
                isNull()
        )).thenReturn(List.of());

        IssueDto result = issueService.createIssue(dto, "john@test.com");

        assertNotNull(result);

        verify(issueRepository).save(any(Issue.class));
        verify(conversationService).createConversationForIssue(any(Issue.class));
        verify(requestManagerRepository).save(any(RequestManager.class));

        verify(documentService).storeFiles(
                anyList(),
                any(Issue.class),
                any(User.class),
                isNull()
        );
    }

    @Test
    void createIssue_should_save_with_INCOMPLET_status_when_pending() {
        // Préparation
        IssueCreateDto dto = new IssueCreateDto();
        dto.setTitle("Brouillon");
        dto.setDescription("Desc");
        dto.setCategoryId(1L);
        dto.setPendingCase(true); // Cas en attente

        User owner = new User();
        owner.setId(1L);
        owner.setEmail("john@test.com");

        Category category = new Category();
        category.setId(1L);
        category.setOwner(new User());

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // Action
        issueService.createIssue(dto, "john@test.com");

        // Assertions pour vérifier que le statut est bien INCOMPLET
        org.mockito.ArgumentCaptor<Issue> issueCaptor = org.mockito.ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(issueCaptor.capture());
        Issue savedIssue = issueCaptor.getValue();

        assertEquals(IssueStatus.INCOMPLET, savedIssue.getStatus());

        verify(conversationService).createConversationForIssue(any(Issue.class));
        verify(requestManagerRepository, never()).save(any());
    }

    @Test
    void createIssue_should_throw_when_user_not_found() {
        IssueCreateDto dto = new IssueCreateDto();
        dto.setCategoryId(1L);

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> issueService.createIssue(dto, "john@test.com"));

        verify(issueRepository, never()).save(any());
    }

    @Test
    void createIssue_should_throw_when_category_not_found() {
        IssueCreateDto dto = new IssueCreateDto();
        dto.setCategoryId(1L);

        User owner = new User();
        owner.setId(1L);
        owner.setEmail("john@test.com");

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class,
                () -> issueService.createIssue(dto, "john@test.com"));

        verify(issueRepository, never()).save(any());
    }

    @Test
    void findAll_should_return_list_of_dto() {

        User categoryOwner = new User();
        categoryOwner.setId(99L);
        categoryOwner.setName("Teacher");

        User user = new User();
        user.setId(1L);
        user.setName("John");

        Category category = new Category();
        category.setId(1L);
        category.setName("Software");
        category.setOwner(categoryOwner);

        Issue issue = Issue.builder()
                .id(1L)
                .title("Bug")
                .description("desc")
                .category(category)
                .createdBy(user)
                .build();

        when(issueRepository.findAll())
                .thenReturn(List.of(issue));

        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.of(user));

        List<IssueDto> result = issueService.findAll("john@test.com");

        assertEquals(1, result.size());
        assertEquals("Bug", result.get(0).getTitle());
        assertEquals(1L, result.get(0).getCategoryId());
        assertEquals("Software", result.get(0).getCategoryName());
    }

    @Test
    void deleteById_should_delete_issue_when_exists() {

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setEmail("john@test.com");

        Issue issue = new Issue();
        issue.setId(1L);
        issue.setCreatedBy(currentUser);

        when(issueRepository.findById(1L))
                .thenReturn(Optional.of(issue));
        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.of(currentUser));

        issueService.deleteById(1L, "john@test.com");

        verify(issueRepository).delete(issue);
    }

    @Test
    void deleteById_should_throw_when_not_found() {

        when(issueRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(IssueNotFoundException.class,
                () -> issueService.deleteById(1L, "john@test.com"));
    }

    @Test
    void deleteById_should_throw_when_user_not_allowed() {

        User currentUser = new User();
        currentUser.setId(2L);
        currentUser.setEmail("other@test.com");

        User creator = new User();
        creator.setId(1L);
        creator.setEmail("john@test.com");

        Issue issue = new Issue();
        issue.setId(1L);
        issue.setCreatedBy(creator);

        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(currentUser));

        assertThrows(AccessDeniedException.class,
                () -> issueService.deleteById(1L, "other@test.com"));

        verify(issueRepository, never()).delete(any());
    }

    @Test
    void toDto_should_map_issue_correctly() {

        User categoryOwner = new User();
        categoryOwner.setId(99L);
        categoryOwner.setName("Teacher");

        User user = new User();
        user.setId(1L);
        user.setName("John");

        Category category = new Category();
        category.setId(1L);
        category.setName("Software");
        category.setOwner(categoryOwner);

        Issue issue = Issue.builder()
                .id(1L)
                .title("Bug")
                .description("desc")
                .status(IssueStatus.OUVERT)
                .category(category)
                .createdBy(user)
                .build();

        IssueDto dto = issueService.toDto(issue);

        assertEquals(1L, dto.getId());
        assertEquals("Bug", dto.getTitle());
        assertEquals("desc", dto.getDescription());
        assertEquals(1L, dto.getCategoryId());
        assertEquals("Software", dto.getCategoryName());
        assertEquals(1L, dto.getCreatedId());
        assertEquals("John", dto.getCreatedName());
    }

    @Test
    void findById_should_return_issue() {

        User categoryOwner = new User();
        categoryOwner.setId(99L);
        categoryOwner.setName("Teacher");

        Category category = Category.builder()
                .id(10L)
                .name("DEV")
                .build();
        category.setOwner(categoryOwner);

        User user = User.builder()
                .id(5L)
                .name("John")
                .build();

        Issue issue = Issue.builder()
                .id(1L)
                .title("Bug")
                .description("desc")
                .category(category)
                .createdBy(user)
                .build();

        when(issueRepository.findById(1L))
                .thenReturn(Optional.of(issue));

        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.of(user));

        IssueDto result = issueService.findById(1L, "john@test.com");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Bug", result.getTitle());
        assertEquals(10L, result.getCategoryId());
        assertEquals("DEV", result.getCategoryName());
        assertEquals(99L, result.getCategoryOwnerId());
        assertEquals("Teacher", result.getCategoryOwnerName());
    }

    @Test
    void findById_should_throw_when_not_found() {

        when(issueRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(IssueNotFoundException.class,
                () -> issueService.findById(1L, "john@test.com"));
    }

    @Test
    void updateStatus_should_update_issue_status() {

        Issue issue = new Issue();
        issue.setId(1L);
        issue.setStatus(IssueStatus.OUVERT);

        when(issueRepository.findById(1L))
                .thenReturn(Optional.of(issue));

        issueService.updateStatus("TERMINE", 1L);

        assertEquals(IssueStatus.TERMINE, issue.getStatus());
        verify(issueRepository).save(issue);
    }

    @Test
    void updateStatus_should_throw_when_invalid_status() {

        assertThrows(IllegalArgumentException.class,
                () -> issueService.updateStatus("INVALID", 1L));
    }

    @Test
    void updateStatus_should_throw_when_issue_not_found() {

        when(issueRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(IssueNotFoundException.class,
                () -> issueService.updateStatus("OUVERT", 99L));
    }

    @Test
    void updatePrivacy_should_toggle_privacy() {

        User currentUser = new User();
        currentUser.setId(10L);

        User categoryOwner = new User();
        categoryOwner.setId(10L);

        Category category = new Category();
        category.setOwner(categoryOwner);

        Issue issue = new Issue();
        issue.setId(1L);
        issue.setPrivate(false);
        issue.setCategory(category);

        when(issueRepository.findById(1L))
                .thenReturn(Optional.of(issue));

        when(userRepository.findByEmail("owner@test.com"))
                .thenReturn(Optional.of(currentUser));

        issueService.updatePrivacy(1L, "owner@test.com");

        assertTrue(issue.isPrivate());
        verify(issueRepository).saveAndFlush(issue);
        verify(issueFollowerService).cleanupFollowersWithoutAccess(1L);
    }

    @Test
    void updatePrivacy_should_toggle_from_true_to_false() {

        User currentUser = new User();
        currentUser.setId(42L);

        User manager = new User();
        manager.setId(42L);

        Issue issue = new Issue();
        issue.setId(1L);
        issue.setPrivate(true);
        issue.setManager(manager);

        when(issueRepository.findById(1L))
                .thenReturn(Optional.of(issue));

        when(userRepository.findByEmail("manager@test.com"))
                .thenReturn(Optional.of(currentUser));

        issueService.updatePrivacy(1L, "manager@test.com");

        assertFalse(issue.isPrivate());
        verify(issueRepository).saveAndFlush(issue);
        verify(issueFollowerService, never()).cleanupFollowersWithoutAccess(1L);
    }

    @Test
    void updatePrivacy_should_throw_when_issue_not_found() {

        when(issueRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(IssueNotFoundException.class,
                () -> issueService.updatePrivacy(99L, "owner@test.com"));
    }

    @Test
    void updatePrivacy_should_throw_when_user_is_not_category_owner_or_manager() {

        User currentUser = new User();
        currentUser.setId(100L);

        User categoryOwner = new User();
        categoryOwner.setId(200L);

        Category category = new Category();
        category.setOwner(categoryOwner);

        Issue issue = new Issue();
        issue.setId(1L);
        issue.setCategory(category);

        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));

        assertThrows(AccessDeniedException.class,
                () -> issueService.updatePrivacy(1L, "user@test.com"));
    }

    @Test
    void assignManagerToIssue_Success() {
        // Précondition
        User testUser = setupUser();
        Issue testIssue = setupIssue();

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
        User testUser = setupUser();

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
        User testUser = setupUser();
        Issue testIssue = setupIssue();

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
        User testUser = setupUser();
        Issue testIssue = setupIssue();
        testUser.setRoles(Set.of(new Role("STUDENT")));

        // Mock
        when(issueRepository.findById(1L)).thenReturn(Optional.of(testIssue));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                issueService.assignManagerToIssue(1L, 2L)
        );
    }

    private User setupUser(){
        User testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.fr");
        return  testUser;
    }

    private Issue setupIssue(){
        User testUser = setupUser();
        Category testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Test Category");

        Issue testIssue = new Issue();
        testIssue.setId(1L);
        testIssue.setTitle("Test Issue");
        testIssue.setDescription("Test Description");
        testIssue.setStatus(IssueStatus.OUVERT);
        testIssue.setCategory(testCategory);
        testIssue.setCreatedBy(testUser);

        return testIssue;
    }

    @Test
    void markIssueAsIncomplete_Success() {
        // Précondtion
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setStatus(IssueStatus.OUVERT);

        RequestManager requestManager = new RequestManager();
        requestManager.setStatus(RequestManagerStatus.PENDING);

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(requestManagerRepository.findFirstByIssueOrderByCreatedAtDesc(issue)).thenReturn(Optional.of(requestManager));

        // Action
        issueService.markIssueAsIncomplete(issueId, commentaire, raison);

        // Assertion
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

        // Action et Assertion
        assertThrows(IllegalStateException.class, () -> {
            issueService.markIssueAsIncomplete(issueId, commentaire, raison);
        });

        // On vérifie qu'aucun save n'a été appelé
        verify(issueRepository, never()).save(any());
        verify(requestManagerRepository, never()).save(any());
    }

    @Test
    void markIssueAsIncomplete_IssueNotFound() {
        // Précondition
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setStatus(IssueStatus.OUVERT);
        when(issueRepository.findById(issueId)).thenReturn(Optional.empty());

        // Action et Assertion
        assertThrows(EntityNotFoundException.class, () -> {
            issueService.markIssueAsIncomplete(issueId, commentaire, raison);
        });

        verify(requestManagerRepository, never()).findFirstByIssueOrderByCreatedAtDesc(issue);
    }

    @Test
    void updateIssueWhenIncomplete_should_update_and_set_OUVERT_when_valid() {
        // Préparation
        Long issueId = 10L;
        String email = "john@test.com";

        IssueCreateDto dto = new IssueCreateDto();
        dto.setTitle("Titre corrigé");
        dto.setDescription("Description corrigée");
        dto.setCategoryId(2L);

        Issue existingIssue = new Issue();
        existingIssue.setId(issueId);
        existingIssue.setStatus(IssueStatus.INCOMPLET);
        existingIssue.setTitle("Vieux titre");

        User owner = new User();
        owner.setId(1L);
        owner.setEmail(email);

        Category newCategory = new Category();
        newCategory.setId(2L);
        newCategory.setName("Nouvelle Catégorie");
        newCategory.setOwner(new User());

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(existingIssue));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(issueRepository.save(any(Issue.class))).thenAnswer(i -> i.getArgument(0));

        // Action
        IssueDto result = issueService.updateIssueWhenIncomplete(email, issueId, dto);

        // Assertions
        assertNotNull(result);

        // Vérification des modifications sur l'entité
        org.mockito.ArgumentCaptor<Issue> issueCaptor = org.mockito.ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(issueCaptor.capture());
        Issue updatedIssue = issueCaptor.getValue();

        assertEquals("Titre corrigé", updatedIssue.getTitle());
        assertEquals("Description corrigée", updatedIssue.getDescription());
        assertEquals(newCategory, updatedIssue.getCategory());
        assertEquals(owner, updatedIssue.getCreatedBy());
        assertEquals(IssueStatus.EN_ATTENTE, updatedIssue.getStatus()); // Doit repasser en attente
    }

    @Test
    void updateIssueWhenIncomplete_should_throw_when_issue_not_found() {
        IssueCreateDto dto = new IssueCreateDto();
        when(issueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IssueNotFoundException.class,
                () -> issueService.updateIssueWhenIncomplete("test@test.fr", 99L, dto));

        verify(issueRepository, never()).save(any());
    }

    @Test
    void updateIssueWhenIncomplete_should_throw_when_user_not_found() {
        Issue existingIssue = new Issue();
        existingIssue.setId(1L);

        when(issueRepository.findById(1L)).thenReturn(Optional.of(existingIssue));
        when(userRepository.findByEmail("ghost@test.fr")).thenReturn(Optional.empty());

        IssueCreateDto dto = new IssueCreateDto();

        assertThrows(EntityNotFoundException.class,
                () -> issueService.updateIssueWhenIncomplete("ghost@test.fr", 1L, dto));

        verify(issueRepository, never()).save(any());
    }

    @Test
    void canAccessIssue_should_return_true_for_public_issue() {
        Issue issue = setupIssue();
        issue.setPrivate(false);
        User user = new User();

        assertTrue(issueService.canAccessIssue(issue, user));
    }

    @Test
    void canAccessIssue_should_return_true_for_private_issue_if_creator() {
        User creator = setupUser();
        creator.setId(1L);
        Issue issue = setupIssue();
        issue.setPrivate(true);
        issue.setCreatedBy(creator);

        assertTrue(issueService.canAccessIssue(issue, creator));
    }

    @Test
    void canAccessIssue_should_return_true_for_private_issue_if_manager() {
        User manager = setupUser();
        manager.setId(2L);
        Issue issue = setupIssue();
        issue.setPrivate(true);
        issue.setManager(manager);

        assertTrue(issueService.canAccessIssue(issue, manager));
    }

    @Test
    void canAccessIssue_should_return_true_for_private_issue_if_category_owner() {
        User owner = setupUser();
        owner.setId(3L);
        Category category = new Category();
        category.setOwner(owner);

        Issue issue = setupIssue();
        issue.setPrivate(true);
        issue.setCategory(category);

        assertTrue(issueService.canAccessIssue(issue, owner));
    }

    @Test
    void canAccessIssue_should_return_false_for_private_issue_if_no_relation() {
        Issue issue = setupIssue();
        issue.setPrivate(true);
        issue.setCreatedBy(setupUser()); // creator id 10L par setupUser
        issue.setManager(null);
        
        Category category = new Category();
        category.setOwner(setupUser());
        category.getOwner().setId(99L);
        issue.setCategory(category);

        User otherUser = setupUser();
        otherUser.setId(5L);

        assertFalse(issueService.canAccessIssue(issue, otherUser));
    }

    @Test
    void canManageIssuePrivacy_should_return_true_for_category_owner() {
        User owner = setupUser();
        owner.setId(1L);
        Category category = new Category();
        category.setOwner(owner);

        Issue issue = setupIssue();
        issue.setCategory(category);

        assertTrue(issueService.canManageIssuePrivacy(issue, owner));
    }

    @Test
    void canManageIssuePrivacy_should_return_false_for_creator_if_not_owner() {
        User creator = setupUser();
        creator.setId(1L);
        Issue issue = setupIssue();
        issue.setCreatedBy(creator);
        issue.setCategory(new Category());
        issue.getCategory().setOwner(setupUser());
        issue.getCategory().getOwner().setId(99L);

        assertFalse(issueService.canManageIssuePrivacy(issue, creator));
    }

    @Test
    void respondManagerRequest_should_accept_and_assign_manager() {
        User teacher = setupUser();
        teacher.setId(1L);
        Issue issue = setupIssue();
        RequestManager rm = new RequestManager();
        rm.setIssue(issue);
        rm.setTargetTeacher(teacher);
        rm.setStatus(RequestManagerStatus.PENDING);

        when(requestManagerRepository.findById(anyLong())).thenReturn(Optional.of(rm));
        when(issueRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        RequestManagerDto result = issueService.respondManagerRequest(true, 10L, 1L);

        assertEquals(RequestManagerStatus.ACCEPTED.name(), result.getStatus());
        assertEquals(teacher.getId(), issue.getManager().getId());
        verify(issueRepository).saveAndFlush(issue);
    }

    @Test
    void respondManagerRequest_should_deny_request() {
        User teacher = setupUser();
        teacher.setId(1L);
        Issue issue = setupIssue();
        issue.setCategory(new Category());
        issue.getCategory().setOwner(new User());

        RequestManager rm = new RequestManager();
        rm.setIssue(issue);
        rm.setTargetTeacher(teacher);
        rm.setStatus(RequestManagerStatus.PENDING);

        when(requestManagerRepository.findById(anyLong())).thenReturn(Optional.of(rm));
        when(requestManagerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RequestManagerDto result = issueService.respondManagerRequest(false, 10L, 1L);

        assertEquals(RequestManagerStatus.DENIED.name(), result.getStatus());
        verify(requestManagerRepository, times(2)).save(any(RequestManager.class));
        verify(issueRepository).saveAndFlush(issue);
    }

    @Test
    void respondManagerRequest_should_throw_when_wrong_user() {
        User teacher = setupUser();
        teacher.setId(1L);
        RequestManager rm = new RequestManager();
        rm.setTargetTeacher(teacher);

        when(requestManagerRepository.findById(anyLong())).thenReturn(Optional.of(rm));

        assertThrows(IllegalStateException.class, () -> issueService.respondManagerRequest(true, 10L, 2L));
    }

    @Test
    void findById_should_throw_when_not_accessible() {
        User user = setupUser();
        user.setEmail("other@test.fr");
        Issue issue = setupIssue();
        issue.setPrivate(true);
        issue.setCreatedBy(new User());
        issue.getCreatedBy().setId(99L);
        issue.setCategory(new Category());
        issue.getCategory().setOwner(new User());
        issue.getCategory().getOwner().setId(88L);

        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(userRepository.findByEmail("other@test.fr")).thenReturn(Optional.of(user));

        assertThrows(AccessDeniedException.class, () -> issueService.findById(1L, "other@test.fr"));
    }

    @Test
    void updatePrivacy_should_throw_when_not_authorized() {
        User user = setupUser();
        user.setEmail("not_owner@test.fr");
        Issue issue = setupIssue();
        issue.setCategory(new Category());
        issue.getCategory().setOwner(new User());
        issue.getCategory().getOwner().setId(99L);

        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(userRepository.findByEmail("not_owner@test.fr")).thenReturn(Optional.of(user));

        assertThrows(AccessDeniedException.class, () -> issueService.updatePrivacy(1L, "not_owner@test.fr"));
    }
}
