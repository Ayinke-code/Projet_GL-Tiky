package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.IssueRelation;
import fr.horizon.backend_api.model.OfficialLinkRequest;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.HandleLinkRequestDto;
import fr.horizon.backend_api.model.dto.OfficialLinkRequestDto;
import fr.horizon.backend_api.model.dto.RelationDto;
import fr.horizon.backend_api.model.dto.RelationRequestDto;
import fr.horizon.backend_api.model.enums.RelationType;
import fr.horizon.backend_api.repository.IssueFollowerRepository;
import fr.horizon.backend_api.repository.IssueRelationRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.OfficialLinkRequestRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class IssueRelationService {

    private final IssueRelationRepository relationRepository;
    private final IssueRepository issueRepository;
    private final IssueFollowerRepository issueFollowerRepository;
    private final OfficialLinkRequestRepository linkRequestRepository;
    private final NotificationService notificationService;

    private static final int OFFICIAL_LINK_THRESHOLD = 5;

    public RelationDto createRelation(Long sourceId, RelationRequestDto dto, User user) {
        Issue source = getIssueOrThrow(sourceId, "source");
        Issue target = getIssueOrThrow(dto.getTargetIssueId(), "cible");

        validateIssuesCompatibility(source, target);
        ensureNoOfficialLinkExists(source.getId(), target.getId());

        boolean isSourceManager = isManagerOf(source, user);
        boolean isTargetManager = isManagerOf(target, user);

        // --- SCÉNARIO 1 : Le manager gère UN SEUL des deux tickets ---
        if (isSourceManager ^ isTargetManager) {
            createOrUpdateDirectManagerRequest(source, target, dto.getType(), isSourceManager, isTargetManager);
            return null;
        }

        boolean isManagerOfBoth = isSourceManager && isTargetManager;

        // --- SCÉNARIO 2 : L'utilisateur gère les DEUX tickets ---
        if (!isManagerOfBoth) {
            ensureStudentFollowsSource(user, source);
        }

        ensureNoDuplicateUserLink(source.getId(), target.getId(), user);

        IssueRelation savedRelation = saveRelation(source, target, dto.getType(), user, isManagerOfBoth);

        // --- SCÉNARIO 3 : Logique de seuil (uniquement pour les étudiants) ---
        if (!isManagerOfBoth) {
            checkOfficialLinkThreshold(source, target, dto.getType());
        }

        return mapToDto(savedRelation);
    }

    /**
     * Permet à un manager de traiter une demande. Nécessite l'accord des deux managers.
     */
    public RelationDto handleOfficialLinkRequest(Long requestId, HandleLinkRequestDto dto, User managerUser) {
        OfficialLinkRequest request = linkRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));

        if (request.isHandled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette demande a déjà été traitée.");
        }

        boolean isSourceManager = request.getSourceIssue().getManager() != null && request.getSourceIssue().getManager().getId().equals(managerUser.getId());
        boolean isTargetManager = request.getTargetIssue().getManager() != null && request.getTargetIssue().getManager().getId().equals(managerUser.getId());

        if (!isSourceManager && !isTargetManager) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul un manager de ces tickets peut traiter la demande.");
        }

        if (!dto.isApprove()) {
            if (dto.getRejectionReason() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un motif de refus est obligatoire.");
            }
            request.setHandled(true);
            request.setApproved(false);
            request.setRejectionReason(dto.getRejectionReason());
            linkRequestRepository.save(request);

            notifyOtherManagerOfResponse(request, isSourceManager, managerUser);
            return null;
        }

        if (isSourceManager) request.setSourceManagerApproved(true);
        if (isTargetManager) request.setTargetManagerApproved(true);

        // Vérification : est-ce que les deux côtés ont approuvé ?
        // (Si un ticket n'a pas de manager assigné, on considère son côté comme "approuvé" par défaut pour ne pas bloquer)
        boolean sourceApproved = request.getSourceIssue().getManager() == null || Boolean.TRUE.equals(request.getSourceManagerApproved());
        boolean targetApproved = request.getTargetIssue().getManager() == null || Boolean.TRUE.equals(request.getTargetManagerApproved());

        if (sourceApproved && targetApproved) {
            // Les deux ont approuvé (ou le même manager a approuvé pour les deux)
            request.setHandled(true);
            request.setApproved(true);
            linkRequestRepository.save(request);

            IssueRelation officialRelation = new IssueRelation();
            officialRelation.setSourceIssue(request.getSourceIssue());
            officialRelation.setTargetIssue(request.getTargetIssue());
            officialRelation.setType(request.getType());
            officialRelation.setManager(true);
            officialRelation.setCreatedBy(managerUser);

            IssueRelation saved = relationRepository.save(officialRelation);

            notifyOtherManagerOfResponse(request, isSourceManager, managerUser);
            notifyAllUniqueFollowers(officialRelation.getSourceIssue(), officialRelation.getTargetIssue(), officialRelation, managerUser);


            return mapToDto(saved);
        } else {
            // Un seul a approuvé, on sauvegarde l'état en attendant l'autre manager
            linkRequestRepository.save(request);
            return null; // Retourne null car le lien officiel n'est pas encore créé
        }
    }

    /**
     * Crée une demande initiée par un étudiant (seuil atteint). Aucun manager n'a encore approuvé.
     */
    private void createOfficialLinkRequestIfNotExists(Issue source, Issue target, RelationType type) {
        boolean requestExists = linkRequestRepository.existsBySourceIssueIdAndTargetIssueIdAndTypeAndIsHandledFalse(source.getId(), target.getId(), type)
                || linkRequestRepository.existsBySourceIssueIdAndTargetIssueIdAndTypeAndIsHandledFalse(target.getId(), source.getId(), type);

        if (!requestExists) {
            OfficialLinkRequest request = OfficialLinkRequest.builder()
                    .sourceIssue(source)
                    .targetIssue(target)
                    .type(type)
                    .isHandled(false)
                    .sourceManagerApproved(null)
                    .targetManagerApproved(null)
                    .build();

            linkRequestRepository.save(request);

            notificationService.newLinkRequestNotification(source.getManager(), request);
            notificationService.newLinkRequestNotification(target.getManager(), request);

        }
    }

    /**
     * Crée une demande initiée directement par le manager d'un des tickets.
     * Le côté du manager initiateur est automatiquement marqué comme approuvé.
     */
    private void createOrUpdateDirectManagerRequest(Issue source, Issue target, RelationType type, boolean isSourceManager, boolean isTargetManager) {

        OfficialLinkRequest request = OfficialLinkRequest.builder()
                .sourceIssue(source)
                .targetIssue(target)
                .type(type)
                .isHandled(false)
                .sourceManagerApproved(isSourceManager ? true : null)
                .targetManagerApproved(isTargetManager ? true : null)
                .build();

        notificationService.newLinkRequestManagerNotification(source.getManager(), request, target.getManager());
        linkRequestRepository.save(request);
    }

    /**
     * Récupère toutes les relations d'un ticket sans filtrage.
     * Le filtrage par rôle est géré côté front via createdById et manager.
     *
     * @param issueId identifiant du ticket
     * @return liste des DTOs de relations
     */
    public List<RelationDto> getRelationsByIssueId(Long issueId) {
        return relationRepository.findAllBySourceIssueIdOrTargetIssueId(issueId, issueId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Récupère la liste des demandes de liens officiels en attente pour un manager.
     * Ne retourne que les demandes liées aux tickets dont l'utilisateur est responsable.
     *
     * @param user L'utilisateur (manager) qui consulte les demandes
     * @return Liste des demandes formatées en DTO
     */
    public List<OfficialLinkRequestDto> getPendingOfficialLinkRequests(User user) {
        List<OfficialLinkRequest> pendingRequests = linkRequestRepository.findPendingRequestsForManager(user.getId());

        return pendingRequests.stream()
                .map(this::mapToOfficialLinkRequestDto)
                .toList();
    }

    /**
     * Supprime définitivement un lien entre deux tickets.
     *
     * @param relationId identifiant unique de la relation à supprimer
     * @throws ResponseStatusException 404 si la relation n'existe pas en base
     */
    public void deleteRelation(Long relationId, User user) {
        IssueRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relation introuvable avec l'id : " + relationId));

        boolean isCreator = relation.getCreatedBy().getId().equals(user.getId());
        boolean isManager = isManager(relation.getSourceIssue(), relation.getTargetIssue(), user);

        if (!isCreator && !isManager) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'avez pas le droit de supprimer ce lien.");
        }

        relationRepository.deleteById(relationId);
    }

    /**
     * Convertit une entité IssueRelation en DTO de transfert.
     */
    private RelationDto mapToDto(IssueRelation relation) {
        RelationDto dto = new RelationDto();
        dto.setId(relation.getId());
        dto.setSourceIssueId(relation.getSourceIssue().getId());
        dto.setTargetIssueId(relation.getTargetIssue().getId());
        dto.setSourceIssueTitle(relation.getSourceIssue().getTitle());
        dto.setTargetIssueTitle(relation.getTargetIssue().getTitle());
        dto.setType(relation.getType());
        dto.setManager(relation.isManager());
        dto.setCreatedById(relation.getCreatedBy() != null ? relation.getCreatedBy().getId() : null);
        dto.setCreatedAt(relation.getCreatedAt());
        return dto;
    }

    /**
     * Convertit une entité OfficialLinkRequest en DTO de transfert.
     */
    private OfficialLinkRequestDto mapToOfficialLinkRequestDto(OfficialLinkRequest request) {
        return OfficialLinkRequestDto.builder()
                .id(request.getId())
                .sourceIssueId(request.getSourceIssue().getId())
                .sourceIssueTitle(request.getSourceIssue().getTitle())
                .targetIssueId(request.getTargetIssue().getId())
                .targetIssueTitle(request.getTargetIssue().getTitle())
                .type(request.getType())
                .sourceManagerId(request.getSourceIssue().getManager() != null ? request.getSourceIssue().getManager().getId() : null)
                .targetManagerId(request.getTargetIssue().getManager() != null ? request.getTargetIssue().getManager().getId() : null)
                .sourceManagerApproved(request.getSourceManagerApproved())
                .targetManagerApproved(request.getTargetManagerApproved())
                .build();
    }

    /**
     * Détermine si un utilisateur est manager du ticket source ou cible.
     *
     * @param source ticket source
     * @param target ticket cible
     * @param user   utilisateur à vérifier
     * @return true si l'utilisateur est manager de l'un des deux tickets
     */
    private boolean isManager(Issue source, Issue target, User user) {
        return (source.getManager() != null && source.getManager().getId().equals(user.getId()))
                || (target.getManager() != null && target.getManager().getId().equals(user.getId()));
    }

    private void checkOfficialLinkThreshold(Issue source, Issue target, RelationType type) {
        int linkCount = relationRepository.countNonManagerRelationsByType(source.getId(), target.getId(), type);
        if (linkCount >= OFFICIAL_LINK_THRESHOLD) {
            createOfficialLinkRequestIfNotExists(source, target, type);

        }
    }

    private IssueRelation saveRelation(Issue source, Issue target, RelationType type, User user, boolean isManager) {
        IssueRelation relation = new IssueRelation();
        relation.setSourceIssue(source);
        relation.setTargetIssue(target);
        relation.setType(type);
        relation.setManager(isManager);
        relation.setCreatedBy(user);

        IssueRelation savedRelation = relationRepository.saveAndFlush(relation);

        notifyAllUniqueFollowers(savedRelation.getSourceIssue(), savedRelation.getTargetIssue(), savedRelation, user);
        return  savedRelation;
    }

    private Issue getIssueOrThrow(Long issueId, String label) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket " + label + " introuvable"));
    }

    private void validateIssuesCompatibility(Issue source, Issue target) {
        if (source.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un ticket ne peut pas être lié à lui-même.");
        }
        if (source.getCategory() == null || target.getCategory() == null
                || !source.getCategory().getId().equals(target.getCategory().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les deux tickets doivent appartenir à la même catégorie.");
        }
    }

    private void ensureNoOfficialLinkExists(Long sourceId, Long targetId) {
        boolean officialExists = relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(sourceId, targetId)
                || relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(targetId, sourceId);
        if (officialExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un lien officiel existe déjà entre ces deux tickets.");
        }
    }

    private void ensureStudentFollowsSource(User user, Issue source) {
        if (!issueFollowerRepository.existsByUserAndIssue(user, source)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous devez suivre ce ticket pour créer une liaison.");
        }
    }

    private void ensureNoDuplicateUserLink(Long sourceId, Long targetId, User user) {
        boolean alreadyLinked = relationRepository.existsBySourceIssueIdAndTargetIssueIdAndCreatedByAndManagerFalse(sourceId, targetId, user)
                || relationRepository.existsBySourceIssueIdAndTargetIssueIdAndCreatedByAndManagerFalse(targetId, sourceId, user);
        if (alreadyLinked) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous avez déjà créé ce lien.");
        }
    }

    private boolean isManagerOf(Issue issue, User user) {
        return issue.getManager() != null && issue.getManager().getId().equals(user.getId());
    }

    /**
     * Méthode utilitaire pour notifier les followers des deux tickets liés.
     * Garantit qu'un utilisateur qui suit les DEUX tickets ne recevra qu'UNE seule notification.
     * Exclut également les managers des deux tickets et la personne qui déclenche l'action.
     */
    private void notifyAllUniqueFollowers(Issue source, Issue target, IssueRelation relation, User actor) {
        List<User> sourceFollowers = issueFollowerRepository.findUsersByIssueId(source.getId());
        List<User> targetFollowers = issueFollowerRepository.findUsersByIssueId(target.getId());

        Set<Long> processedUserIds = new HashSet<>();

        processedUserIds.add(actor.getId());
        if (source.getManager() != null) processedUserIds.add(source.getManager().getId());
        if (target.getManager() != null) processedUserIds.add(target.getManager().getId());

        for (User follower : sourceFollowers) {
            if (!processedUserIds.contains(follower.getId())) {
                notificationService.followersNewLinkNotification(follower, source, relation);
                processedUserIds.add(follower.getId());
            }
        }

        for (User follower : targetFollowers) {
            if (!processedUserIds.contains(follower.getId())) {
                notificationService.followersNewLinkNotification(follower, target, relation);
                processedUserIds.add(follower.getId());
            }
        }
    }

    /**
     * Notifie l'autre manager du résultat de la demande de liaison (acceptée ou refusée).
     */
    private void notifyOtherManagerOfResponse(OfficialLinkRequest request, boolean isSourceManager, User managerUser) {
        User otherManager = isSourceManager ? request.getTargetIssue().getManager() : request.getSourceIssue().getManager();

        // On s'assure que l'autre manager existe, et on évite de s'auto-notifier si on gère les deux tickets
        if (otherManager != null && !otherManager.getId().equals(managerUser.getId())) {
            notificationService.linkIssueResponseNotification(otherManager, request);
        }
    }
}