package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.IssueFollower;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.FollowedReason;
import fr.horizon.backend_api.repository.IssueFollowerRepository;
import fr.horizon.backend_api.repository.IssueRepository;
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

@ExtendWith(MockitoExtension.class)
class IssueFollowerServiceTest {

    @Mock
    private IssueFollowerRepository issueFollowerRepository;

    @Mock
    private IssueRepository issueRepository;

    @InjectMocks
    private IssueFollowerService issueFollowerService;

    private User user;
    private Issue issue;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        issue = new Issue();
        issue.setIdTick(1L);
    }

    @Test
    void followIssue_success() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.existsByCreateurAndIssue(user, issue)).thenReturn(false);
        when(issueFollowerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        IssueFollower result = issueFollowerService.followIssue(1L, "INTERESTED", user);

        assertNotNull(result);
        assertEquals(FollowedReason.INTERESTED, result.getRaison());
        verify(issueFollowerRepository, times(1)).save(any());
    }

    @Test
    void followIssue_alreadyFollowing_throwsConflict() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.existsByCreateurAndIssue(user, issue)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () ->
                issueFollowerService.followIssue(1L, "INTERESTED", user));
    }

    @Test
    void followIssue_invalidReason_throwsBadRequest() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.existsByCreateurAndIssue(user, issue)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () ->
                issueFollowerService.followIssue(1L, "INVALID_REASON", user));
    }

    @Test
    void followIssue_issueNotFound_throwsNotFound() {
        when(issueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                issueFollowerService.followIssue(99L, "INTERESTED", user));
    }

    @Test
    void isFollowing_true() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.existsByCreateurAndIssue(user, issue)).thenReturn(true);

        boolean result = issueFollowerService.isFollowing(1L, user);

        assertTrue(result);
    }

    @Test
    void isFollowing_false() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.existsByCreateurAndIssue(user, issue)).thenReturn(false);

        boolean result = issueFollowerService.isFollowing(1L, user);

        assertFalse(result);
    }

    @Test
    void canInteract_withAbleToHelp_returnsTrue() {
        IssueFollower follower = new IssueFollower();
        follower.setRaison(FollowedReason.ABLE_TO_HELP);

        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.findByCreateurAndIssue(user, issue))
                .thenReturn(Optional.of(follower));

        boolean result = issueFollowerService.canInteract(1L, user);

        assertTrue(result);
    }

    @Test
    void canInteract_withInterested_returnsFalse() {
        IssueFollower follower = new IssueFollower();
        follower.setRaison(FollowedReason.INTERESTED);

        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.findByCreateurAndIssue(user, issue))
                .thenReturn(Optional.of(follower));

        boolean result = issueFollowerService.canInteract(1L, user);

        assertFalse(result);
    }

    @Test
    void getFollowedIssues_returnsList() {
        IssueFollower follower = new IssueFollower();
        follower.setIssue(issue);

        when(issueFollowerRepository.findByCreateur(user)).thenReturn(List.of(follower));

        List<Issue> result = issueFollowerService.getFollowedIssues(user);

        assertEquals(1, result.size());
        assertEquals(issue, result.get(0));
    }
}