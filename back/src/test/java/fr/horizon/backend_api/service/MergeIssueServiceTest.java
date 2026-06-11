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
    @Mock private DocumentRepository documentRepository;
    @Mock private NotificationService notificationService;

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


    @Test
    void sendMergeRequest_ShouldThrowConflict_WhenSourceAlreadyMerged() {
        sourceIssue.setMerged(true);

        when(userRepository.findByEmail("test@horizon.fr"))
                .thenReturn(Optional.of(testUser));
        when(issueRepository.findById(10L))
                .thenReturn(Optional.of(sourceIssue));
        when(issueRepository.findById(20L))
                .thenReturn(Optional.of(targetIssue));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> mergeIssueService.sendMergeRequest(10L, 20L, "test@horizon.fr")
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void sendMergeRequest_ShouldThrowConflict_WhenPendingRequestAlreadyExists() {

        when(userRepository.findByEmail("test@horizon.fr"))
                .thenReturn(Optional.of(testUser));

        when(issueRepository.findById(10L))
                .thenReturn(Optional.of(sourceIssue));

        when(issueRepository.findById(20L))
                .thenReturn(Optional.of(targetIssue));

        when(mergeRequestRepository.existsBySourceIssueIdAndTargetIssueIdAndStatus(
                10L,
                20L,
                MergeRequestStatus.PENDING))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> mergeIssueService.sendMergeRequest(10L, 20L, "test@horizon.fr")
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void sendMergeRequest_ShouldMergeDirectly_WhenManagerOwnsBothIssues() {

        sourceIssue.setManager(testUser);
        targetIssue.setManager(testUser);

        when(userRepository.findByEmail("test@horizon.fr"))
                .thenReturn(Optional.of(testUser));

        when(issueRepository.findById(10L))
                .thenReturn(Optional.of(sourceIssue));

        when(issueRepository.findById(20L))
                .thenReturn(Optional.of(targetIssue));

        when(conversationRepository.findByIssueId(anyLong()))
                .thenReturn(Optional.empty());

        when(conversationRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        when(mergeRequestRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        MergeRequestDto result =
                mergeIssueService.sendMergeRequest(10L, 20L, "test@horizon.fr");

        assertEquals(MergeRequestStatus.ACCEPTED, result.getStatus());

        verify(issueRepository, atLeastOnce()).save(any(Issue.class));
    }

    @Test
    void respondToMergeRequest_ShouldThrowBadRequest_WhenRejectReasonMissing() {

        sourceIssue.setManager(testUser);

        MergeRequest request = MergeRequest.builder()
                .id(100L)
                .sourceIssue(sourceIssue)
                .targetIssue(targetIssue)
                .requester(testUser)
                .status(MergeRequestStatus.PENDING)
                .build();

        when(userRepository.findByEmail("test@horizon.fr"))
                .thenReturn(Optional.of(testUser));

        when(mergeRequestRepository.findById(100L))
                .thenReturn(Optional.of(request));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> mergeIssueService.respondToMergeRequest(
                        100L,
                        false,
                        "",
                        "test@horizon.fr")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void respondToMergeRequest_ShouldThrowBadRequest_WhenAlreadyHandled() {

        MergeRequest request = MergeRequest.builder()
                .id(100L)
                .status(MergeRequestStatus.ACCEPTED)
                .sourceIssue(sourceIssue)
                .targetIssue(targetIssue)
                .requester(testUser)
                .build();

        when(userRepository.findByEmail("test@horizon.fr"))
                .thenReturn(Optional.of(testUser));

        when(mergeRequestRepository.findById(100L))
                .thenReturn(Optional.of(request));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> mergeIssueService.respondToMergeRequest(
                        100L,
                        true,
                        null,
                        "test@horizon.fr")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void respondToMergeRequest_ShouldRemainPending_WhenOnlyOneManagerApproves() {

        User sourceManager = new User();
        sourceManager.setId(99L);

        sourceIssue.setManager(sourceManager);

        User targetManager = new User();
        targetManager.setId(100L);

        targetIssue.setManager(targetManager);

        MergeRequest request = MergeRequest.builder()
                .id(100L)
                .status(MergeRequestStatus.PENDING)
                .sourceIssue(sourceIssue)
                .targetIssue(targetIssue)
                .requester(testUser)
                .build();

        when(userRepository.findByEmail("manager@horizon.fr"))
                .thenReturn(Optional.of(sourceManager));

        when(mergeRequestRepository.findById(100L))
                .thenReturn(Optional.of(request));

        when(mergeRequestRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        MergeRequestDto dto =
                mergeIssueService.respondToMergeRequest(
                        100L,
                        true,
                        null,
                        "manager@horizon.fr");

        assertEquals(MergeRequestStatus.PENDING, dto.getStatus());
        assertTrue(dto.getSourceManagerApproved());
        assertNull(dto.getTargetManagerApproved());
    }

    @Test
    void respondToMergeRequest_ShouldMerge_WhenSecondManagerApproves() {

        User sourceManager = new User();
        sourceManager.setId(99L);

        User targetManager = new User();
        targetManager.setId(100L);

        sourceIssue.setManager(sourceManager);
        targetIssue.setManager(targetManager);

        MergeRequest request = MergeRequest.builder()
                .id(100L)
                .status(MergeRequestStatus.PENDING)
                .sourceIssue(sourceIssue)
                .targetIssue(targetIssue)
                .requester(testUser)
                .sourceManagerApproved(true)
                .build();

        when(userRepository.findByEmail("manager2@horizon.fr"))
                .thenReturn(Optional.of(targetManager));

        when(mergeRequestRepository.findById(100L))
                .thenReturn(Optional.of(request));

        when(conversationRepository.findByIssueId(anyLong()))
                .thenReturn(Optional.empty());

        when(conversationRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        when(mergeRequestRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        MergeRequestDto result =
                mergeIssueService.respondToMergeRequest(
                        100L,
                        true,
                        null,
                        "manager2@horizon.fr");

        assertEquals(MergeRequestStatus.ACCEPTED, result.getStatus());

        verify(issueRepository, atLeastOnce()).save(any(Issue.class));
    }

    @Test
    void getManagerRequestDto_ShouldReturnPendingRequests() {

        when(userRepository.findByEmail("test@horizon.fr"))
                .thenReturn(Optional.of(testUser));

        MergeRequest request = MergeRequest.builder()
                .id(100L)
                .sourceIssue(sourceIssue)
                .targetIssue(targetIssue)
                .requester(testUser)
                .status(MergeRequestStatus.PENDING)
                .build();

        when(mergeRequestRepository.findPendingRequestsForManager(1L))
                .thenReturn(java.util.List.of(request));

        var result = mergeIssueService.getManagerRequestDto("test@horizon.fr");

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
    }

    @Test
    void getRecentDeniedRequests_ShouldReturnRejectedRequests() {

        when(userRepository.findByEmail("test@horizon.fr"))
                .thenReturn(Optional.of(testUser));

        MergeRequest request = MergeRequest.builder()
                .id(100L)
                .sourceIssue(sourceIssue)
                .targetIssue(targetIssue)
                .requester(testUser)
                .status(MergeRequestStatus.REJECTED)
                .rejectionReason("Refus")
                .build();

        when(mergeRequestRepository.findDeniedRequestsForManager(
                eq(1L),
                any()))
                .thenReturn(java.util.List.of(request));

        var result =
                mergeIssueService.getRecentDeniedRequests("test@horizon.fr");

        assertEquals(1, result.size());
        assertEquals(MergeRequestStatus.REJECTED, result.get(0).getStatus());
    }
}