package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.dto.RelationDto;
import fr.horizon.backend_api.model.dto.RelationRequestDto;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.IssueRelation;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.IssueRelationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Service gérant les liens de parenté et les dépendances entre les issues.
 * Permet de lier des tickets connexes ou de déclarer des blocages fonctionnels.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IssueRelationService {

    private final IssueRelationRepository relationRepository;
    private final IssueRepository issueRepository;

    /**
     * Crée une relation de dépendance ou d'association entre deux issues distinctes.
     *
     * @param sourceId identifiant de l'issue émettrice
     * @param dto      contient l'identifiant de la cible et le type (LINKED, DEPENDS_ON)
     * @return le DTO de la relation créée enrichi des titres des issues
     * @throws ResponseStatusException 400 si l'issue tente de se lier à elle-même
     * @throws ResponseStatusException 409 si la relation exacte existe déjà
     * @throws ResponseStatusException 404 si l'issue source ou cible est introuvable
     */
    public RelationDto createRelation(Long sourceId, RelationRequestDto dto) {
        if (sourceId.equals(dto.getTargetIssueId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une issue ne peut pas être liée à elle-même.");
        }

        boolean exists = relationRepository.existsBySourceIssueIdAndTargetIssueIdAndType(
                sourceId, dto.getTargetIssueId(), dto.getType());
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce lien existe déjà.");
        }

        Issue source = issueRepository.findById(sourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue source introuvable"));
        Issue target = issueRepository.findById(dto.getTargetIssueId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue cible introuvable"));

        IssueRelation relation = new IssueRelation();
        relation.setSourceIssue(source);
        relation.setTargetIssue(target);
        relation.setType(dto.getType());

        IssueRelation saved = relationRepository.save(relation);
        return mapToDto(saved);
    }

    /**
     * Récupère l'ensemble des relations impliquant une issue, qu'elle soit source ou cible.
     * S'appuie sur la dérivation automatique de requête Spring Data JPA.
     *
     * @param issueId identifiant de l'issue concernée
     * @return la liste des DTOs des relations associées
     */
    @Transactional(readOnly = true)
    public List<RelationDto> getRelationsByIssueId(Long issueId) {
        return relationRepository.findAllBySourceIssueIdOrTargetIssueId(issueId, issueId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Supprime définitivement un lien de relation entre deux issues.
     *
     * @param relationId identifiant unique de la relation à rompre
     * @throws ResponseStatusException 404 si la relation n'existe pas en base
     */
    public void deleteRelation(Long relationId) {
        if (!relationRepository.existsById(relationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Relation introuvable avec l'id : " + relationId);
        }
        relationRepository.deleteById(relationId);
    }

    /**
     * Convertit une entité IssueRelation en son DTO de transfert, avec résolution des titres.
     */
    private RelationDto mapToDto(IssueRelation relation) {
        RelationDto dto = new RelationDto();
        dto.setId(relation.getId());
        dto.setSourceIssueId(relation.getSourceIssue().getId());
        dto.setTargetIssueId(relation.getTargetIssue().getId());
        dto.setSourceIssueTitle(relation.getSourceIssue().getTitle());
        dto.setTargetIssueTitle(relation.getTargetIssue().getTitle());
        dto.setType(relation.getType());
        return dto;
    }
}