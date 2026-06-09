package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.IssueNotFoundException;
import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.MergeRequestDto;
import fr.horizon.backend_api.model.enums.MergeRequestStatus;
import fr.horizon.backend_api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MergeIssueService {

    private final IssueRepository issueRepository;
    private final MergeRequestRepository mergeRequestRepository;
    private final UserRepository userRepository;
    private final IssueFollowerRepository issueFollowerRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final DocumentRepository documentRepository;
    private final NotificationService notificationService;

    private static final String USER_NOT_FOUND = "Utilisateur introuvable";

    /**
     * Initie une demande de fusion.
     * Si l'initiateur est manager d'un des tickets, son côté est pré-approuvé.
     */
    public MergeRequestDto sendMergeRequest(Long sourceId, Long targetId, String email) {
        if (sourceId.equals(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de fusionner un ticket avec lui-même.");
        }

        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        Issue source = issueRepository.findById(sourceId)
                .orElseThrow(() -> new IssueNotFoundException(sourceId));
        Issue target = issueRepository.findById(targetId)
                .orElseThrow(() -> new IssueNotFoundException(targetId));

        if (source.isMerged() || target.isMerged()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'un de ces tickets est déjà fusionné.");
        }

        if (mergeRequestRepository.existsBySourceIssueIdAndTargetIssueIdAndStatus(sourceId, targetId, MergeRequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une demande de fusion est déjà en cours pour ces tickets.");
        }

        boolean isSourceManager = isManagerOf(source, requester);
        boolean isTargetManager = isManagerOf(target, requester);

        MergeRequest request = MergeRequest.builder()
                .sourceIssue(source)
                .targetIssue(target)
                .requester(requester)
                .status(MergeRequestStatus.PENDING)
                .sourceManagerApproved(isSourceManager ? true : null)
                .targetManagerApproved(isTargetManager ? true : null)
                .build();

        // Si le manager gère les 2 tickets, la fusion est immédiate
        boolean sourceApproved = source.getManager() == null || Boolean.TRUE.equals(request.getSourceManagerApproved());
        boolean targetApproved = target.getManager() == null || Boolean.TRUE.equals(request.getTargetManagerApproved());

        if (sourceApproved && targetApproved) {
            request.setStatus(MergeRequestStatus.ACCEPTED);
            request.setTargetManager(requester);
            mergeRequestRepository.save(request);

            mergeIssues(source, target);
            return mapToDto(request);
        }

        MergeRequest saved = mergeRequestRepository.save(request);
        notificationService.mergeRequestNotification(request);
        return mapToDto(saved);
    }

    /**
     * Traite la demande de fusion par un manager (Accepter ou Refuser).
     */
    public MergeRequestDto respondToMergeRequest(Long mergeRequestId, boolean accept, String reason, String email) {
        User manager = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        MergeRequest request = mergeRequestRepository.findById(mergeRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));

        if (request.getStatus() != MergeRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette demande a déjà été traitée.");
        }

        boolean isSourceManager = isManagerOf(request.getSourceIssue(), manager);
        boolean isTargetManager = isManagerOf(request.getTargetIssue(), manager);

        if (!isSourceManager && !isTargetManager) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul un manager concerné peut valider cette demande.");
        }

        request.setTargetManager(manager);

        if (!accept) {
            if (reason == null || reason.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un motif de refus est obligatoire.");
            }
            request.setStatus(MergeRequestStatus.REJECTED);
            request.setRejectionReason(reason);
            MergeRequest saved = mergeRequestRepository.save(request);
            notificationService.mergeRequestDeniedNotification(request);
            return mapToDto(saved);
        }

        if (isSourceManager) request.setSourceManagerApproved(true);
        if (isTargetManager) request.setTargetManagerApproved(true);

        boolean sourceApproved = request.getSourceIssue().getManager() == null || Boolean.TRUE.equals(request.getSourceManagerApproved());
        boolean targetApproved = request.getTargetIssue().getManager() == null || Boolean.TRUE.equals(request.getTargetManagerApproved());

        if (sourceApproved && targetApproved) {
            request.setStatus(MergeRequestStatus.ACCEPTED);
            mergeIssues(request.getSourceIssue(), request.getTargetIssue());
            notificationService.mergeRequestAcceptedNotification(request);
        }

        MergeRequest saved = mergeRequestRepository.save(request);
        return mapToDto(saved);
    }

    /**
     * Récupère les demandes en attente pour un manager.
     */
    public List<MergeRequestDto> getManagerRequestDto(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        return mergeRequestRepository.findPendingRequestsForManager(user.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<MergeRequestDto> getRecentDeniedRequests(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        java.time.LocalDateTime thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30);

        return mergeRequestRepository.findDeniedRequestsForManager(user.getId(), thirtyDaysAgo)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Logique interne de fusion des tickets.
     * Transfère les abonnés et clôture le ticket source.
     */
    private void mergeIssues(Issue source, Issue target) {
        source.setMerged(true);
        source.setMergedInto(target);

        source.setMergedAt(LocalDateTime.now());

        source.setRejectComment("Ce ticket a été fusionné dans le ticket #" + target.getId());

        // Ceux qui suivaient le ticket A suivent désormais automatiquement le ticket B
        List<IssueFollower> sourceFollowers = source.getFollowers();
        for (IssueFollower follower : sourceFollowers) {
            boolean alreadyFollowsTarget = issueFollowerRepository.existsByUserAndIssue(follower.getUser(), target);
            if (!alreadyFollowsTarget) {
                IssueFollower newFollower = new IssueFollower();
                newFollower.setUser(follower.getUser());
                newFollower.setIssue(target);
                newFollower.setReason(follower.getReason());
                target.getFollowers().add(newFollower);
            }
        }

        // On s'assure que le ticket cible possède bien une conversation
        Conversation targetConversation = conversationRepository.findByIssueId(target.getId())
                .orElseGet(() -> {
                    Conversation newConv = Conversation.builder().issue(target).build();
                    return conversationRepository.save(newConv);
                });

        // On récupère la conversation du ticket source
        Optional<Conversation> sourceConversationOpt = conversationRepository.findByIssueId(source.getId());

        if (sourceConversationOpt.isPresent()) {
            Conversation sourceConversation = sourceConversationOpt.get();

            List<Message> sourceMessages = messageRepository.findByConversationOrderByCreatedAtAsc(sourceConversation);

            for (Message msg : sourceMessages) {
                msg.setConversation(targetConversation);
            }

            messageRepository.saveAll(sourceMessages);
        }

        // on associe les documents de la source
        documentRepository.moveDocumentsToNewIssue(source, target);
        if (source.getDocuments() != null) {
            source.getDocuments().clear();
        }

        // on passe la source a privé si le ticket avant était privé
        if(source.isPrivate()){
            target.setPrivate(true);
        }

        issueRepository.save(source);
        issueRepository.save(target);
    }


    /**
     * Helper de vérification des droits manager.
     */
    private boolean isManagerOf(Issue issue, User user) {
        return issue.getManager() != null && issue.getManager().getId().equals(user.getId());
    }

    /**
     *  mapper MergeRequest vers un DTO.
     */
    private MergeRequestDto mapToDto(MergeRequest request) {
        MergeRequestDto dto = new MergeRequestDto();
        dto.setId(request.getId());
        dto.setSourceIssueId(request.getSourceIssue().getId());
        dto.setSourceIssueTitle(request.getSourceIssue().getTitle());
        dto.setTargetIssueId(request.getTargetIssue().getId());
        dto.setTargetIssueTitle(request.getTargetIssue().getTitle());
        dto.setRequesterId(request.getRequester().getId());
        dto.setStatus(request.getStatus());
        dto.setRejectionReason(request.getRejectionReason());
        dto.setSourceManagerApproved(request.getSourceManagerApproved());
        dto.setTargetManagerApproved(request.getTargetManagerApproved());
        if (request.getSourceIssue().getManager() != null) {
            dto.setSourceManagerId(request.getSourceIssue().getManager().getId());
        }
        if (request.getTargetIssue().getManager() != null) {
            dto.setTargetManagerId(request.getTargetIssue().getManager().getId());
        }
        return dto;
    }
}