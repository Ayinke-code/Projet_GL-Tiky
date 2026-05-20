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
    }

    /** Vérifie qu'un utilisateur peut suivre une issue avec succès */
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

    /** Vérifie qu'une exception CONFLICT est levée si l'utilisateur suit déjà l'issue */
    @Test
    void followIssue_alreadyFollowing_throwsConflict() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.existsByCreateurAndIssue(user, issue)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () ->
                issueFollowerService.followIssue(1L, "INTERESTED", user));
    }

    /** Vérifie qu'une exception BAD_REQUEST est levée si la raison est invalide */
    @Test
    void followIssue_invalidReason_throwsBadRequest() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueFollowerRepository.existsByCreateurAndIssue(user, issue)).thenReturn(false);

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
}