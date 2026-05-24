package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.CategoryNotFoundException;
import fr.horizon.backend_api.exception.IssueNotFoundException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.RequestManager;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.IssueCreateDto;
import fr.horizon.backend_api.model.dto.IssueDto;
import fr.horizon.backend_api.model.dto.RequestManagerDto;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.RequestManagerRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Retourne la liste de toutes les issues.
     *
     * @return liste de DTO issues
     */
    public List<IssueDto> findAll() {
        return issueRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Retourne l'issue correspondant à l'id.
     *
     * @param id id de l'issue à trouver
     * @return DTO de l'issue
     */
    public IssueDto findById(Long id) {
        return issueRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IssueNotFoundException(id));
    }

    /**
     * Crée une nouvelle issue.
     * <p>
     * L'issue est associée à un utilisateur (owner) via son email
     * et à une catégorie existante.
     * Crée automatiquement une conversation pour cette issue.
     *
     * @param dto   données de création de l'issue
     * @param email email de l'utilisateur créateur
     * @return issue créée sous forme de DTO
     * @throws RuntimeException si l'utilisateur n'existe pas
     * @throws CategoryNotFoundException si la catégorie n'existe pas
     */
    @Transactional
    public IssueDto createIssue(IssueCreateDto dto, String email) {

        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(dto.getCategoryId()));

        Issue issue = Issue.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(category)
                .status(IssueStatus.EN_ATTENTE)
                .createdBy(owner)
                .build();

        if(dto.isPendingCase()){
            issue.setStatus(IssueStatus.INCOMPLET);
        }

        issueRepository.save(issue);

        if (!dto.isPendingCase()) {
            createRequestManager(issue);
        }

        // Créer automatiquement une conversation pour cette issue
        conversationService.createConversationForIssue(issue);

        return toDto(issue);
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
        IssueStatus issueStatus;

        try {
            issueStatus = IssueStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status");
        }

        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id));

        issue.setStatus(issueStatus);

        issueRepository.save(issue);
    }

    /**
     * Inverse la visibilité privée d'une issue.
     * <p>
     * Passe une issue de privé à public ou inversement.
     *
     * @param id identifiant de l'issue
     * @throws IssueNotFoundException si l'issue n'existe pas
     */
    public void updatePrivacy(Long id){
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id));

        issue.setPrivate(!issue.isPrivate());

        issueRepository.save(issue);
    }

    /**
     * Supprime une issue par son identifiant.
     *
     * @param id identifiant de l'issue
     * @throws IssueNotFoundException si l'issue n'existe pas
     */
    @Transactional
    public void deleteById(Long id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id));

        issueRepository.delete(issue);
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
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));

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

        return toDto(issue);
    }


    /**
     * Convertit une entité Issue en DTO.
     *
     * @param issue entité issue
     * @return DTO issue
     */
    public IssueDto toDto(Issue issue) {
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
                // CreatedBy
                .createdId(issue.getCreatedBy().getId())
                .createdName(issue.getCreatedBy().getName())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
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
            requestManager.setProcessingDate(LocalDateTime.now());
            initialStatus = RequestManagerStatus.ACCEPTED;
            issue.setStatus(IssueStatus.OUVERT);
        }

        requestManager.setTargetTeacher(targetedUser);
        requestManager.setStatus(initialStatus);

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

        requestManager.setProcessingDate(LocalDateTime.now());

        if (response) {
            requestManager.setStatus(RequestManagerStatus.ACCEPTED);
            requestManager.getIssue().setManager(requestManager.getTargetTeacher());
            requestManager.getIssue().setStatus(IssueStatus.OUVERT);
        } else {
            requestManager.setStatus(RequestManagerStatus.DENIED);
            createRequestManager(requestManager.getIssue());
        }

        issueRepository.save(requestManager.getIssue());
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
        issueRepository.save(issue);

        RequestManager requestManager = requestManagerRepository.findFirstByIssueOrderByCreatedAtDesc(issue)
                .orElseThrow(() -> new EntityNotFoundException("Aucune demande associée à l'issue " + idIssue));

        requestManager.setStatus(RequestManagerStatus.DENIED);
        requestManager.setProcessingDate(LocalDateTime.now());
        requestManagerRepository.save(requestManager);
    }


    public IssueDto getIssueDTOById(Long id) {
        return toDto(issueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Issue non trouvée")));
    }

    public List<IssueDto> getIncompleteIssuesToResent(Long userId) {
        List<Issue> issues = issueRepository.findByStatusAndCreatedBy_Id(IssueStatus.INCOMPLET, userId);

        return issues.stream()
                .map(this::toDto)
                .toList();
    }




}
