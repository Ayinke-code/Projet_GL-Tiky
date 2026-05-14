package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.IssueDto;
import fr.horizon.backend_api.model.dto.RequestManagerDto;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import fr.horizon.backend_api.repository.RequestManagerRepository;
import fr.horizon.backend_api.repository.IssueRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
@Transactional
public class IssueService {

    private final IssueRepository issueRepository;
    private final RequestManagerRepository requestManagerRepository;


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

        List<RequestManager> merge = Stream.concat(requestApproved.stream(), requestDenied.stream())
                .sorted(Comparator.comparing(RequestManager::getCreatedAt).reversed())
                .toList();

        return merge.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Marquer une issue comme imcomplète
     */
    public void markIssueAsIncomplete(Long idIssue) {
        Issue issue = issueRepository.findById(idIssue)
                .orElseThrow(() -> new EntityNotFoundException("Issue introuvable avec l'ID : " + idIssue));

        if (issue.getStatus() == IssueStatus.TERMINE) {
            throw new IllegalStateException("Impossible de marquer comme incomplète une issue déjà résolue.");
        }

        issue.setStatus(IssueStatus.INCOMPLET);
        issueRepository.save(issue);

        RequestManager requestManager = requestManagerRepository.findByIssue_Id(idIssue)
                .orElseThrow(() -> new EntityNotFoundException("Aucune demande associée à l'issue " + idIssue));

        requestManager.setStatus(RequestManagerStatus.DENIED);
        requestManagerRepository.save(requestManager);
    }


    public IssueDto getIssueDTOById(Long id) {
        return convertIssueToDTO(issueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Issue non trouvée")));
    }

    public List<IssueDto> getIncompleteIssuesToResent(Long userId) {
        List<Issue> issues = issueRepository.findByStatusAndCreatedBy_Id(IssueStatus.INCOMPLET, userId);

        return issues.stream()
                .map(this::convertIssueToDTO)
                .collect(Collectors.toList());
    }

    private IssueDto convertIssueToDTO(Issue issue) {
        IssueDto dto = new IssueDto();
        dto.setTitle(issue.getTitle());
        dto.setDescription(issue.getDescription());
        dto.setCategory(issue.getCategory().getName());
        dto.setCategoryId(issue.getCategory() != null ? issue.getCategory().getId() : null);
        dto.setCreator(issue.getCreatedBy().getName());
        return dto;
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
}
