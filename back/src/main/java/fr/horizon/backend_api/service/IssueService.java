package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.RequestManagerDto;
import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.RequestManagerRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;



@Service
@RequiredArgsConstructor
@Transactional
public class IssueService {

    private final RequestManagerRepository requestManagerRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;


    /**
     * envoyer une demande d'assignation d'un ticket à un teacher
     * @param userId Id de l'enseignant visé par la demande.
     * @param issueId Id du ticket visé par la demande.
     * @return le DTO d'une demande d'assignation
     */
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
            initialStatus = RequestManagerStatus.ACCEPTED;
            issue.setManager(targetedUser);
            requestManager.setProcessingDate(LocalDateTime.now());
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
        List<RequestManager> entities = requestManagerRepository.findByCategoryOwner_IdAndTargetTeacherIsNull(userId);

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
                // Renvoyer NULL si pas de prof, c'est ce que le JS attend avec !r.target
                .target(entity.getTargetTeacher() != null ? entity.getTargetTeacher().getName() : null)
                // Renvoyer le nom technique de l'Enum (PENDING, ACCEPTED, etc.)
                .status(entity.getStatus() != null ? entity.getStatus().name() : "PENDING")
                .creationDate(entity.getCreatedAt())
                .processingDate(entity.getProcessingDate())
                .issueTitle(entity.getIssue().getTitle())
                .creator(entity.getIssue().getCreatedBy().getName())
                .build();
    }

    private void createRequestManager(Issue issue){
        RequestManager requestManager = RequestManager.builder()
                .issue(issue)
                .categoryOwner(issue.getCategory().getOwner())
                .build();

        requestManagerRepository.save(requestManager);
    }

    /**
     * récupérer la liste des demandes assignés
     * @param userId Id du categorie Owner
     * @return le DTO d'une demande d'assignation
     */
    public List<RequestManagerDto> getHistoricalRequestManager(long userId) {
        List<RequestManager> entities = requestManagerRepository
                .findByCategoryOwner_IdAndTargetTeacherIsNotNullOrderByCreatedAtDesc(userId);

        return entities.stream()
                .map(this::toDto)
                .toList();
    }


}
