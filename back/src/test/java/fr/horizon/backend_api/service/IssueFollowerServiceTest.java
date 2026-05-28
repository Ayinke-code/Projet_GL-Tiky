package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.IssueFollower;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.enums.FollowedReason;
import fr.horizon.backend_api.repository.IssueFollowerRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour IssueFollowerService.
 * Vérifie les cas nominaux et les cas d'erreur pour chaque méthode du service.
 */
@ExtendWith(MockitoExtension.class)
class IssueFollowerServiceTest {

    @Mock
    private IssueFollowerRepository issueFollowerRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private IssueFollowerService issueFollowerService;

    private User user;
    private Issue issue;

    /** Initialise les objets de test avant chaque test */
    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        issue = new Issue();
        issue.setId(1L);
        issue.setStatus(IssueStatus.OUVERT);
    }

    /** Vérifie que getUserByEmail retourne l'utilisateur si trouvé */
    @Test
    void getUserByEmail_success() {
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(user));

        User result = issueFollowerService.getUserByEmail("test@test.com");

        assertEquals(user, result);
    }

    /** Vérifie qu'une exception NOT_FOUND est levée si l'email n'existe pas */
    @Test
    void getUserByEmail_notFound_throwsNotFound() {
        when(userRepository.findByEmail("inconnu@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                issueFollowerService.getUserByEmail("inconnu@test.com"));
    }

    /** Vérifie qu'un utilisateur peut suivre une issue avec succès */
    @Test
    void followIssue_success() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.existsByUserAndIssue(user, issue)).thenReturn(false);
        when(issueFollowerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        IssueFollower result = issueFollowerService.followIssue(1L, "INTERESTED", user);

        assertNotNull(result);
        assertEquals(FollowedReason.INTERESTED, result.getReason());
        verify(issueFollowerRepository, times(1)).save(any());
    }

    /** Vérifie qu'une exception CONFLICT est levée si l'utilisateur suit déjà l'issue */
    @Test
    void followIssue_alreadyFollowing_throwsConflict() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.existsByUserAndIssue(user, issue)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () ->
                issueFollowerService.followIssue(1L, "INTERESTED", user));
    }

    /** Vérifie qu'une exception BAD_REQUEST est levée si la raison est invalide */
    @Test
    void followIssue_invalidReason_throwsBadRequest() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.existsByUserAndIssue(user, issue)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () ->
                issueFollowerService.followIssue(1L, "INVALID_REASON", user));
    }

    /** Vérifie qu'une exception NOT_FOUND est levée si l'issue n'existe pas */
    @Test
    void followIssue_issueNotFound_throwsNotFound() {
        when(issueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                issueFollowerService.followIssue(99L, "INTERESTED", user));
    }

    /** Vérifie qu'un utilisateur peut se désabonner d'une issue */
    @Test
    void unfollowIssue_success() {
        IssueFollower follower = new IssueFollower();

        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.findByUserAndIssue(user, issue))
                .thenReturn(Optional.of(follower));

        issueFollowerService.unfollowIssue(1L, user);

        verify(issueFollowerRepository, times(1)).delete(follower);
    }

    /** Vérifie qu'une exception NOT_FOUND est levée si l'abonnement n'existe pas */
    @Test
    void unfollowIssue_notFollowing_throwsNotFound() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.findByUserAndIssue(user, issue))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                issueFollowerService.unfollowIssue(1L, user));
    }

    /** Vérifie que isFollowing retourne true si l'utilisateur suit l'issue */
    @Test
    void isFollowing_true() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.existsByUserAndIssue(user, issue)).thenReturn(true);

        boolean result = issueFollowerService.isFollowing(1L, user);

        assertTrue(result);
    }

    /** Vérifie que isFollowing retourne false si l'utilisateur ne suit pas l'issue */
    @Test
    void isFollowing_false() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.existsByUserAndIssue(user, issue)).thenReturn(false);

        boolean result = issueFollowerService.isFollowing(1L, user);

        assertFalse(result);
    }

    /** Vérifie que getFollowedIssues retourne bien la liste des issues suivies */
    @Test
    void getFollowedIssues_returnsList() {
        when(issueFollowerRepository.deleteInaccessibleFollowedIssuesByUser(1L))
                .thenReturn(0);
        when(issueFollowerRepository.findAccessibleFollowedIssues(1L, IssueStatus.OUVERT))
                .thenReturn(List.of(issue));

        List<Issue> result = issueFollowerService.getFollowedIssues(user);

        assertEquals(1, result.size());
        assertEquals(issue, result.get(0));
    }

    /** Vérifie que getFollowedIssues retire automatiquement les followers sans accès à une issue privée */
    @Test
    void getFollowedIssues_removeFollowerWithoutAccessToPrivateIssue() {
        // Setup issue with category
        Category category = new Category();
        User otherUser = new User();
        otherUser.setId(999L);
        otherUser.setName("Other User");

        // Issue privée
        issue.setPrivate(true);
        issue.setCreatedBy(otherUser);
        issue.setManager(otherUser);
        category.setOwner(otherUser);
        issue.setCategory(category);

        // L'utilisateur courant (user) ne l'a pas créée et n'est pas manager
            when(issueFollowerRepository.deleteInaccessibleFollowedIssuesByUser(1L))
                .thenReturn(1);
        when(issueFollowerRepository.findAccessibleFollowedIssues(1L, IssueStatus.OUVERT))
                .thenReturn(List.of());

        List<Issue> result = issueFollowerService.getFollowedIssues(user);

        // L'issue ne doit pas être retournée
        assertTrue(result.isEmpty());
        // Le follower doit être supprimé via la requête de purge
        verify(issueFollowerRepository).deleteInaccessibleFollowedIssuesByUser(1L);
    }

    /** Vérifie que cleanupFollowersWithoutAccess supprime bien les followers sans droits */
    @Test
    void cleanupFollowersWithoutAccess_removeRestrictedFollowers() {
        // Setup
        Category category = new Category();
        User creator = new User();
        creator.setId(100L);
        User manager = new User();
        manager.setId(200L);
        User categoryOwner = new User();
        categoryOwner.setId(300L);
        User otherUser = new User();
        otherUser.setId(999L);

        // Issue privée
        issue.setPrivate(true);
        issue.setCreatedBy(creator);
        issue.setManager(manager);
        category.setOwner(categoryOwner);
        issue.setCategory(category);

        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.deleteInaccessibleFollowedIssuesByIssue(1L, 100L))
                .thenReturn(1);

        issueFollowerService.cleanupFollowersWithoutAccess(1L);

        // La purge doit se faire via la requête dédiée
        verify(issueFollowerRepository).deleteInaccessibleFollowedIssuesByIssue(1L, 100L);
    }
}