package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.exception.IssueNotFoundException;
import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.DocumentDto;
import fr.horizon.backend_api.model.dto.IssueCreateDto;
import fr.horizon.backend_api.model.dto.IssueDto;
import fr.horizon.backend_api.model.dto.RequestManagerDto;
import fr.horizon.backend_api.model.enums.FollowedReason;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import fr.horizon.backend_api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service de gestion des issues.
 * <p>
 * Fournit les opérations métier liées aux issues :
 * création, récupération, suppression et conversion en DTO.
 * <p>
 * Gère les relations entre Issue, User (owner) et Category,
 * ainsi que les règles de création et de suppression.
 */
@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ConversationService conversationService;
    private final RequestManagerRepository requestManagerRepository;
    private final IssueFollowerRepository issueFollowerRepository;
    private final IssueFollowerService issueFollowerService;
    private final NotificationService notificationService;
    private final DocumentService documentService;
    private final EventRepository eventRepository;
    private final MergeRequestRepository mergeRequestRepository;
    private final IssueRelationRepository issueRelationRepository;

    private static final String USER_NOT_FOUND = "User not found";

    private static final String OWNER_NF = "Owner not found";

    @Value("${app.upload-dir}")
    private String uploadDir;
    /**
     * Retourne la liste de toutes les issues.
     *
     * @return liste de DTO issues
     */
    public List<IssueDto> findAll(String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        return issueRepository.findAll()
                .stream()
                .filter(issue -> canAccessIssue(issue, currentUser))
                .map(this::toDto)
                .toList();
    }

    /**
     * Retourne l'issue correspondant à l'id.
     *
     * @param id id de l'issue à trouver
     * @return DTO de l'issue
     */
    public IssueDto findById(Long id, String email) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id));

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        if (!canAccessIssue(issue, currentUser)) {
            throw new AccessDeniedException("Vous n'avez pas accès à cette issue privée");
        }

        return toDto(issue);
    }

    /**
     * Crée une issue sans fichiers attachés.
     *
     * @param dto   données de création de l'issue
     * @param email email de l'utilisateur créateur
     * @return issue créée sous forme de DTO
     */
    @Transactional
    public IssueDto createIssue(IssueCreateDto dto, String email) {
        return createIssueInternal(dto, List.of(), email);
    }

    /**
     * Crée une issue avec fichiers attachés.
     *
     * @param dto   données de création de l'issue
     * @param files fichiers à attacher à l'issue
     * @param email email de l'utilisateur créateur
     * @return issue créée sous forme de DTO
     */
    @Transactional
    public IssueDto createIssue(IssueCreateDto dto, List<MultipartFile> files, String email) {
        return createIssueInternal(dto, files, email);
    }

    /**
     * Logique commune de création d'une issue.
     * <p>
     * Construit l'issue, la persiste, abonne automatiquement le créateur,
     * puis exécute les actions post-création (gestionnaire, notification,
     * conversation, fichiers).
     *
     * @param dto   données de création de l'issue
     * @param files fichiers à attacher (peut être vide)
     * @param email email de l'utilisateur créateur
     * @return issue créée sous forme de DTO
     */
    private IssueDto createIssueInternal(IssueCreateDto dto, List<MultipartFile> files, String email) {
        Issue issue = buildIssue(dto, email);
        issueRepository.save(issue);
        autoFollowCreator(issue);
        postCreateActions(dto, files, issue);
        return toDto(issue);
    }

    /**
     * Construit une instance {@link Issue} à partir des données de création.
     * <p>
     * Récupère le créateur et la catégorie depuis la base de données,
     * puis initialise le statut de l'issue selon qu'il s'agit d'un cas
     * incomplet ou non.
     *
     * @param dto   données de création de l'issue
     * @param email email de l'utilisateur créateur
     * @return instance Issue non encore persistée
     * @throws EntityNotFoundException   si le créateur n'est pas trouvé
     * @throws CategoryNotFoundException si la catégorie n'est pas trouvée
     */
    private Issue buildIssue(IssueCreateDto dto, String email) {

        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(OWNER_NF));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(dto.getCategoryId()));

        Issue issue = Issue.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(category)
                .status(IssueStatus.EN_ATTENTE)
                .createdBy(owner)
                .build();

        if (dto.isPendingCase()) {
            issue.setStatus(IssueStatus.INCOMPLET);
        }

        return issue;
    }

    /**
     * Abonne automatiquement le créateur de l'issue en tant que gestionnaire.
     * <p>
     * Si le créateur n'est pas déjà abonné, un {@link IssueFollower} est créé
     * avec la raison {@link FollowedReason#IS_MANAGER}.
     *
     * @param issue issue déjà persistée
     */
    private void autoFollowCreator(Issue issue) {
        User owner = issue.getCreatedBy();
        if (!issueFollowerRepository.existsByUserAndIssue(owner, issue)) {
            IssueFollower follower = IssueFollower.builder()
                    .issue(issue)
                    .user(owner)
                    .reason(FollowedReason.IS_MANAGER)
                    .build();
            issueFollowerRepository.save(follower);
        }
    }

    /**
     * Exécute les actions post-création d'une issue.
     * <p>
     * Si l'issue n'est pas un cas incomplet :
     * <ul>
     *   <li>crée le gestionnaire de demande associé</li>
     *   <li>envoie une notification de création au responsable de la catégorie</li>
     * </ul>
     * Dans tous les cas, crée la conversation liée à l'issue et traite
     * les éventuels fichiers attachés.
     *
     * @param dto   données de création de l'issue
     * @param files fichiers à attacher (peut être vide)
     * @param issue issue déjà persistée
     */
    private void postCreateActions(IssueCreateDto dto, List<MultipartFile> files, Issue issue) {

        if (!dto.isPendingCase()) {
            createRequestManager(issue);
            notificationService.creationTicketNotification(issue.getCategory().getOwner(), issue);
        }

        conversationService.createConversationForIssue(issue);

        documentService.storeFiles(files, issue, issue.getCreatedBy(), null);
    }

    /**
     * Met à jour le statut d'une issue.
     * <p>
     * Convertit la chaîne reçue en valeur de l'enum {@link IssueStatus}.
     * Si la valeur est invalide, une exception est levée.
     *
     * @param status nouveau statut sous forme de chaîne
     * @param id identifiant de l'issue
     * @throws IllegalArgumentException si le statut est invalide
     * @throws IssueNotFoundException si l'issue n'existe pas
     */
    public void updateStatus(String status, Long id) {
        IssueStatus newStatus;

        try {
            newStatus = IssueStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status");
        }

        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id));

        IssueStatus oldStatus = issue.getStatus();

        issue.setStatus(newStatus);
        issue.setUpdatedAt(LocalDateTime.now());
        issueRepository.save(issue);

        if (oldStatus == IssueStatus.DEMANDE_REOUVERTURE && newStatus == IssueStatus.OUVERT) {
            notificationService.reopeningDemandAcceptedNotification(issue.getCreatedBy(), issue);
        }
        if (oldStatus == IssueStatus.OUVERT && newStatus == IssueStatus.TERMINE) {
            notificationService.closeIssueNotification(issue.getCreatedBy(), issue);
        }
        if(oldStatus == IssueStatus.TERMINE && newStatus == IssueStatus.OUVERT ){
            notificationService.openIssueNotification(issue.getCreatedBy(), issue);
        }

    }

    /**
     * Demande la réouverture d'une issue terminée.
     * <p>
     * Met à jour le statut en {@link IssueStatus#DEMANDE_REOUVERTURE}
     * et enregistre la description fournie par l'utilisateur.
     *
     * @param description justification de la demande de réouverture
     * @param id identifiant de l'issue
     * @throws IssueNotFoundException si l'issue n'existe pas
     * @throws IllegalStateException si l'issue n'est pas au statut TERMINE
     * @throws IllegalArgumentException si la description est vide
     */
    public void requestReopening(String description, Long id){
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id));

        if (issue.getStatus() != IssueStatus.TERMINE) {
            throw new IllegalStateException("L'issue doit être terminée pour demander une réouverture");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("La description est obligatoire");
        }

        issue.setStatus(IssueStatus.DEMANDE_REOUVERTURE);
        issue.setReopeningDescription(description);
        notificationService.reopeningDemandNotification(issue.getManager(), issue);

        issueRepository.save(issue);
    }

    /**
     * Inverse la visibilité privée d'une issue.
     * <p>
     * Passe une issue de privé à public ou inversement.
     * Si l'issue devient privée, nettoie les followers qui n'ont plus accès.
     *
     * @param id identifiant de l'issue
     * @throws IssueNotFoundException si l'issue n'existe pas
     */
    @Transactional
    public void updatePrivacy(Long id, String email){
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id));

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        if (!canManageIssuePrivacy(issue, currentUser)) {
            throw new AccessDeniedException("Seul le category owner ou l'issue manager peut modifier la visibilité");
        }

        // Nettoyer les followers AVANT de rendre l'issue privée
        boolean willBecomePrivate = !issue.isPrivate();
        
        issue.setPrivate(!issue.isPrivate());
        issue.setUpdatedAt(LocalDateTime.now());
        issueRepository.saveAndFlush(issue);

        // Nettoyer les followers qui n'ont plus accès si l'issue vient de devenir privée
        if (willBecomePrivate) {
            issueFollowerService.cleanupFollowersWithoutAccess(id);
        }
    }

    /**
     * Supprime une issue par son identifiant.
     *
     * @param id identifiant de l'issue
     * @throws IssueNotFoundException si l'issue n'existe pas
     */
    @Transactional
    public void deleteById(Long id, String email) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id));

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        if (!canDeleteIssue(issue, currentUser)) {
            throw new AccessDeniedException("Vous n'avez pas l'autorisation de supprimer cette issue");
        }
        // Supprimer les demande de fusion et les liens
        mergeRequestRepository.deleteByIssueId(id);
        issueRelationRepository.deleteByIssueId(id);
        // Supprimer les événements liés avant de supprimer l'issue
        eventRepository.deleteByIssueId(id);

        // Supprime tous les documents liés à l'issue qui va être supprimé
        documentService.deleteDocumentsByIssueId(id);

        issueRepository.delete(issue);
    }

    /**
     * Deletes all issues associated with the given category,
     * along with their related events and documents.
     *
     * @param category the category whose issues must be deleted
     */
    @Transactional
    public void deleteAllByCategory(Category category) {
        List<Issue> issues = issueRepository.findByCategoryId(category.getId());

        if (issues.isEmpty()) return;

        List<Long> issueIds = issues.stream()
                .map(Issue::getId)
                .toList();

        // Supprime tous les événements liés en une seule requête
        eventRepository.deleteByIssueIdIn(issueIds);

        // Supprime tous les documents liés en une seule requête
        documentService.deleteDocumentsByIssueIdIn(issueIds);

        // Supprime toutes les issues en une seule requête
        issueRepository.deleteAll(issues);
    }

    /**
     * Met à jour une issue lorsqu'elle est corrigé suite à une issue imcomplète et relance une demande de manager/validation
     *
     * @param email identifiant de l'utilisateur
     * @param dto champ mis à jour pour la création de l'issue
     * @param id identifiant de l'issue
     */
    @Transactional
    public IssueDto updateIssueWhenIncomplete(String email,Long id, IssueCreateDto dto){

        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id));

        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(OWNER_NF));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(dto.getCategoryId()));


        issue.setTitle(dto.getTitle());
        issue.setDescription(dto.getDescription());
        issue.setCategory(category);
        issue.setCreatedBy(owner);
        // réouverture de la demande
        issue.setStatus(IssueStatus.EN_ATTENTE);

        issueRepository.save(issue);

        createRequestManager(issue);

        notificationService.creationTicketNotification(category.getOwner(), issue);

        return toDto(issue);
    }


    /**
     * Convertit une entité Issue en DTO.
     *
     * @param issue entité issue
     * @return DTO issue
     */
    public IssueDto toDto(Issue issue) {
        // Vérifie si un manager est assigné à l'issue,
        // pour récupérer ou non l'id et le nom
        User manager = issue.getManager();

        return IssueDto.builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .status(issue.getStatus())
                .isPrivate(issue.isPrivate())
                .rejectType(issue.getRejectType())
                .rejectReason(issue.getRejectReason())
                .rejectComment(issue.getRejectComment())
                // Category
                .categoryId(issue.getCategory().getId())
                .categoryName(issue.getCategory().getName())
                .categoryOwnerId(issue.getCategory().getOwner().getId())
                .categoryOwnerName(issue.getCategory().getOwner().getName())
                // Category manager
                .categoryManagerId(manager != null ? manager.getId() : null)
                .categoryManagerName(manager != null ? manager.getName() : null)
                // CreatedBy
                .createdId(issue.getCreatedBy().getId())
                .createdName(issue.getCreatedBy().getName())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                // request reopening
                .reopeningDescription(issue.getReopeningDescription())
                //manager
                .teacherManagerName(manager != null ? manager.getName() : null)
                //follower
                .followersCount(countFollower(issue))
                // documents
                .documents(
                        issue.getDocuments()
                                .stream()
                                .map(this::toDocumentDto)
                                .toList()
                )
                .build();
    }

    /**
     * Convertit une entité Issue en DTO Document.
     *
     * @param document entité document
     * @return DTO document
     */
    private DocumentDto toDocumentDto(Document document) {
        return DocumentDto.builder()
                .id(document.getId())
                .originalName(document.getOriginalName())
                .contentType(document.getContentType())
                .size(document.getSize())
                .createdAt(document.getCreatedAt())
                .build();
    }

    public boolean canAccessIssue(Issue issue, User user) {
        if (!issue.isPrivate()) {
            return true;
        }

        Long currentUserId = user.getId();
        Long creatorId = issue.getCreatedBy() != null ? issue.getCreatedBy().getId() : null;
        Long categoryOwnerId = issue.getCategory() != null && issue.getCategory().getOwner() != null
                ? issue.getCategory().getOwner().getId()
                : null;
        Long categoryManagerId = issue.getManager() != null ? issue.getManager().getId() : null;

        return currentUserId != null && (
                currentUserId.equals(creatorId)
                        || currentUserId.equals(categoryOwnerId)
                        || currentUserId.equals(categoryManagerId)
        );
    }

    public boolean canManageIssuePrivacy(Issue issue, User user) {
        Long currentUserId = user.getId();
        Long categoryOwnerId = issue.getCategory() != null && issue.getCategory().getOwner() != null
                ? issue.getCategory().getOwner().getId()
                : null;
        Long categoryManagerId = issue.getManager() != null ? issue.getManager().getId() : null;

        return currentUserId != null && (
                currentUserId.equals(categoryOwnerId)
                        || currentUserId.equals(categoryManagerId)
        );
    }

    private boolean canDeleteIssue(Issue issue, User user) {
        Long currentUserId = user.getId();
        Long creatorId = issue.getCreatedBy() != null ? issue.getCreatedBy().getId() : null;
        Long categoryOwnerId = issue.getCategory() != null && issue.getCategory().getOwner() != null
                ? issue.getCategory().getOwner().getId()
                : null;
        Long categoryManagerId = issue.getManager() != null ? issue.getManager().getId() : null;

        return currentUserId != null && (
                currentUserId.equals(creatorId)
                        || currentUserId.equals(categoryOwnerId)
                        || currentUserId.equals(categoryManagerId)
        );
    }


    /**
     * envoyer une demande d'assignation d'un ticket à un teacher
     * @param userId Id de l'enseignant visé par la demande.
     * @param issueId Id du ticket visé par la demande.
     * @return le DTO d'une demande d'assignation
     */
    @Transactional
    public RequestManagerDto assignManagerToIssue(Long issueId, Long userId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket non trouvé"));

        User teacher = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        if (requestManagerRepository.existsByIssueAndStatusAndTargetTeacherIsNotNull(issue, RequestManagerStatus.PENDING)) {
            throw new IllegalStateException("Une demande de manager est déjà en cours pour ce ticket.");
        }

        if (issue.getManager() != null) {
            throw new IllegalStateException("Ce ticket a déjà un manager assigné.");
        }

        if (!teacher.isTeacher()) {
            throw new IllegalArgumentException("L'utilisateur visé n'est pas un teacher");
        }

        return sendManagerRequest(issue, teacher);
    }

    /**
     * gère l'envoie d'une demande d'assignation d'un ticket à un teacher
     * @param issue Ticket visé par la demande.
     * @param targetedUser Teacher visé par la demande.
     * @return le DTO d'une demande d'assignation
     */
    private RequestManagerDto sendManagerRequest(Issue issue, User targetedUser) {
        RequestManager requestManager = requestManagerRepository.findFirstByIssueOrderByCreatedAtDesc(issue)
                .orElseThrow(() -> new EntityNotFoundException("Demande initiale introuvable"));

        RequestManagerStatus initialStatus = RequestManagerStatus.PENDING;

        if (targetedUser.getId().equals(issue.getCategory().getOwner().getId())) {
            issue.setManager(targetedUser);

            if (!issueFollowerRepository.existsByUserAndIssue(targetedUser, issue)) {
                IssueFollower follower = IssueFollower.builder()
                        .issue(issue)
                        .user(targetedUser)
                        .reason(FollowedReason.IS_MANAGER)
                        .build();
                issueFollowerRepository.save(follower);
            }

            requestManager.setProcessingDate(LocalDateTime.now());
            initialStatus = RequestManagerStatus.ACCEPTED;
            issue.setStatus(IssueStatus.OUVERT);
            // notification du créateur
            notificationService.validatationTicketNotification(issue.getCreatedBy(), issue);
        }

        requestManager.setTargetTeacher(targetedUser);
        requestManager.setStatus(initialStatus);
        if(!targetedUser.getId().equals(issue.getCategory().getOwner().getId())){
        notificationService.reveivedManagerRequest(targetedUser,requestManager);
        }

        return toDto(requestManagerRepository.save(requestManager));
    }

    /**
     * répondre à une demande d'assignation d'un ticket à un teacher et met à jour le status de la demande
     * @param userId Id de l'enseignant qui répond à la demande.
     * @param demandeManagerId Id de la demande d'assignation du ticket.
     * @param response Réponse de l'enseignant.
     * @return le DTO d'une demande d'assignation
     */
    @Transactional
    public RequestManagerDto respondManagerRequest(boolean response, Long demandeManagerId, Long userId) {
        RequestManager requestManager = requestManagerRepository.findById(demandeManagerId)
                .orElseThrow(() -> new IllegalArgumentException("Demande non trouvée"));

        if (!requestManager.getTargetTeacher().getId().equals(userId)) {
            throw new IllegalStateException("Vous n'êtes pas autorisé à répondre à cette demande");
        }

        Issue issue = requestManager.getIssue();
        User teacher = requestManager.getTargetTeacher();

        requestManager.setProcessingDate(LocalDateTime.now());

        if (response) {
            requestManager.setStatus(RequestManagerStatus.ACCEPTED);
            issue.setManager(teacher);
            issue.setStatus(IssueStatus.OUVERT);
            //notification du créateur
            notificationService.validatationTicketNotification(issue.getCreatedBy(), issue);
            // notifcation pour le categorie Owner que sa demande a été accepté
            notificationService.managerDemandNotification(requestManager.getCategoryOwner(), requestManager);


            if (!issueFollowerRepository.existsByUserAndIssue(teacher, issue)) {
                IssueFollower follower = IssueFollower.builder()
                        .issue(issue)
                        .user(teacher)
                        .reason(FollowedReason.IS_MANAGER)
                        .build();
                issueFollowerRepository.save(follower);
            }

        } else {
            requestManager.setStatus(RequestManagerStatus.DENIED);
            notificationService.managerDemandNotification(requestManager.getCategoryOwner(), requestManager);
            createRequestManager(issue);
        }

        requestManagerRepository.save(requestManager);
        issueRepository.saveAndFlush(issue);
        issueFollowerService.cleanupFollowersWithoutAccess(issue.getId());
        return toDto(requestManager);
    }


    /**
     * Récupére un RequestManager en DTO pour un utilsateur spécifique.
     */
    public List<RequestManagerDto> getTeacherAssignations(Long userId) {
        List<RequestManager> entities = requestManagerRepository.findByTargetTeacher_Id(userId);

        return entities.stream()
                .map(this::toDto)
                .toList();
    }



    public List<RequestManagerDto> alertCategoryOwner(Long userId){
        List<RequestManager> entities = requestManagerRepository.findByCategoryOwner_IdAndTargetTeacherIsNullAndStatus(userId, RequestManagerStatus.PENDING);

        return entities.stream()
                .map(this::toDto)
                .toList();
    }


    /**
     * Transforme un RequestManager en DTO.
     * Si l'entité est nouvelle (pas encore de date de création), on peut passer une date par défaut.
     */
    public RequestManagerDto toDto(RequestManager entity) {
        return RequestManagerDto.builder()
                .id(entity.getRequestId())
                // Renvoyer NULL si pas de prof
                .target(entity.getTargetTeacher() != null ? entity.getTargetTeacher().getName() : null)
                // Renvoyer le nom technique de l'Enum (PENDING, ACCEPTED, etc.)
                .status(entity.getStatus() != null ? entity.getStatus().name() : "PENDING")
                .creationDate(entity.getCreatedAt())
                .issueId(entity.getIssue().getId())
                .processingDate(entity.getProcessingDate())
                .issueTitle(entity.getIssue().getTitle())
                .creator(entity.getIssue().getCreatedBy().getName())
                .build();
    }

    private void createRequestManager(Issue issue){
        RequestManager requestManager = RequestManager.builder()
                .issue(issue)
                .categoryOwner(issue.getCategory().getOwner())
                .status(RequestManagerStatus.PENDING)
                .build();

        requestManagerRepository.save(requestManager);
    }

    /**
     * récupérer la liste des demandes assignés
     * @param userId Id du categorie Owner
     * @return le DTO d'une demande d'assignation
     */
    public List<RequestManagerDto> getHistoricalRequestManager(long userId) {
        List<RequestManager> requestApproved = requestManagerRepository
                .findByCategoryOwner_IdAndTargetTeacherIsNotNullOrderByCreatedAtDesc(userId);

        List<RequestManager> requestDenied = requestManagerRepository
                .findByCategoryOwner_IdAndTargetTeacherIsNullAndStatusOrderByCreatedAtDesc(userId, RequestManagerStatus.DENIED);

        return Stream.concat(requestApproved.stream(), requestDenied.stream())
                .sorted(Comparator.comparing(RequestManager::getUpdatedAt).reversed())
                .limit(5)
                .map(this::toDto)
                .toList();
    }

    /**
     * Marquer une issue comme imcomplète
     */
    @Transactional
    public void markIssueAsIncomplete(Long idIssue, String commentaire, String raison) {
        Issue issue = issueRepository.findById(idIssue)
                .orElseThrow(() -> new EntityNotFoundException("Issue introuvable avec l'ID : " + idIssue));

        if (issue.getStatus() == IssueStatus.TERMINE) {
            throw new IllegalStateException("Impossible de marquer comme incomplète une issue déjà résolue.");
        }
        issue.setRejectComment(commentaire);
        issue.setRejectReason(raison);
        issue.setStatus(IssueStatus.INCOMPLET);
        notificationService.incompleteTicketNotification(issue.getCreatedBy(), issue);
        issueRepository.save(issue);

        RequestManager requestManager = requestManagerRepository.findFirstByIssueOrderByCreatedAtDesc(issue)
                .orElseThrow(() -> new EntityNotFoundException("Aucune demande associée à l'issue " + idIssue));

        requestManager.setStatus(RequestManagerStatus.DENIED);
        requestManager.setProcessingDate(LocalDateTime.now());
        requestManagerRepository.save(requestManager);
    }


    public IssueDto getIssueDTOById(Long id, String email) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Issue non trouvée"));

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        if (!canAccessIssue(issue, currentUser)) {
            throw new AccessDeniedException("Vous n'avez pas accès à cette issue privée");
        }

        return toDto(issue);
    }

    public List<IssueDto> getIncompleteIssuesToResent(Long userId) {
        List<Issue> issues = issueRepository.findByStatusAndCreatedBy_Id(IssueStatus.INCOMPLET, userId);

        return issues.stream()
                .map(this::toDto)
                .toList();
    }


    /**
     * récupérer la liste des issues à afficher dans la page index des issues
     * @param email mail de l'utilisateur
     * @return la liste des DTO des issues à afficher
     */
    public List<IssueDto> getAllDisplayableIssue(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(OWNER_NF));

        List<IssueStatus> openStatuses = List.of(IssueStatus.OUVERT);

        List<Issue> displayableIssues = issueRepository.findDisplayableIssues(user, openStatuses);

        return displayableIssues.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Compte le nombre d'abonnés pour un ticket donné.
     * * @param issue l'entité ticket
     * @return le nombre total d'abonnés
     */
    private int countFollower(Issue issue) {
        if (issue == null || issue.getId() == null) {
            return 0;
        }
        return (int) issueFollowerRepository.countByIssue(issue);
    }





}
