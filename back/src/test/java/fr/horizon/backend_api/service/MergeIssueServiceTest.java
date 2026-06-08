package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.MergeRequestDto;
import fr.horizon.backend_api.model.enums.MergeRequestStatus;
import fr.horizon.backend_api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MergeIssueServiceTest {

    @Mock private IssueRepository issueRepository;
    @Mock private MergeRequestRepository mergeRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private IssueFollowerRepository issueFollowerRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;

    @InjectMocks
    private MergeIssueService mergeIssueService;

    private User testUser;
    private Issue sourceIssue;
    private Issue targetIssue;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@horizon.fr");

        User manager = new User();
        manager.setId(99L);

        sourceIssue = new Issue();
        sourceIssue.setId(10L);
        sourceIssue.setTitle("Source Issue");
        sourceIssue.setManager(manager);

        targetIssue = new Issue();
        targetIssue.setId(20L);
        targetIssue.setTitle("Target Issue");
        targetIssue.setManager(manager);
    }

    @Test
    void sendMergeRequest_ShouldThrowException_WhenIdsAreIdentical() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                mergeIssueService.sendMergeRequest(10L, 10L, "test@horizon.fr")
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void sendMergeRequest_ShouldCreatePendingRequest_WhenUserIsNotManager() {

        when(userRepository.findByEmail("test@horizon.fr")).thenReturn(Optional.of(testUser));
        when(issueRepository.findById(10L)).thenReturn(Optional.of(sourceIssue));
        when(issueRepository.findById(20L)).thenReturn(Optional.of(targetIssue));

        MergeRequest savedRequest = MergeRequest.builder()
                .id(100L).sourceIssue(sourceIssue).targetIssue(targetIssue)
                .requester(testUser).status(MergeRequestStatus.PENDING).build();

        when(mergeRequestRepository.save(any(MergeRequest.class))).thenReturn(savedRequest);

        MergeRequestDto result = mergeIssueService.sendMergeRequest(10L, 20L, "test@horizon.fr");

        // Assertion
        assertNotNull(result);
        assertEquals(MergeRequestStatus.PENDING, result.getStatus());
        verify(mergeRequestRepository).save(any(MergeRequest.class));
    }

    @Test
    void respondToMergeRequest_ShouldReject_WhenManagerRefuses() {
        MergeRequest request = MergeRequest.builder()
                .id(100L).sourceIssue(sourceIssue).targetIssue(targetIssue)
                .requester(testUser).status(MergeRequestStatus.PENDING).build();

        sourceIssue.setManager(testUser);

        when(userRepository.findByEmail("test@horizon.fr")).thenReturn(Optional.of(testUser));
        when(mergeRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(mergeRequestRepository.save(any(MergeRequest.class))).thenAnswer(i -> i.getArguments()[0]);

        // Action
        MergeRequestDto result = mergeIssueService.respondToMergeRequest(100L, false, "Pas pertinent", "test@horizon.fr");

        // Assertion
        assertEquals(MergeRequestStatus.REJECTED, result.getStatus());
        assertEquals("Pas pertinent", result.getRejectionReason());
        verify(mergeRequestRepository).save(request);
    }

    @Test
    void respondToMergeRequest_ShouldThrowForbidden_WhenUserIsNotManager() {
        User anotherUser = new User();
        anotherUser.setId(2L);

        MergeRequest request = MergeRequest.builder()
                .id(100L).sourceIssue(sourceIssue).targetIssue(targetIssue)
                .requester(testUser).status(MergeRequestStatus.PENDING).build();

        when(userRepository.findByEmail("notmanager@horizon.fr")).thenReturn(Optional.of(anotherUser));
        when(mergeRequestRepository.findById(100L)).thenReturn(Optional.of(request));

        // Action
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                mergeIssueService.respondToMergeRequest(100L, true, null, "notmanager@horizon.fr")
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
}