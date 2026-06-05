package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.dto.RelationDto;
import fr.horizon.backend_api.model.dto.RelationRequestDto;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.IssueRelation;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.repository.IssueFollowerRepository;
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
 * Seuls les abonnés de l'issue source peuvent créer une liaison.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IssueRelationService {

    private final IssueRelationRepository relationRepository;
    private final IssueRepository issueRepository;
    private final IssueFollowerRepository issueFollowerRepository;

    /**
     * Crée une relation de dépendance ou d'association entre deux issues distinctes.
     * L'utilisateur doit suivre l'issue source pour pouvoir créer le lien.
     *
     * @param sourceId identifiant de l'issue émettrice
     * @param dto      contient l'identifiant de la cible et le type (LINKED, DEPENDS_ON)
     * @param user     utilisateur effectuant l'action
     * @return le DTO de la relation créée enrichi des titres des issues
     * @throws ResponseStatusException 403 si l'utilisateur ne suit pas l'issue source
     * @throws ResponseStatusException 400 si l'issue tente de se lier à elle-même
     * @throws ResponseStatusException 409 si un lien existe déjà entre ces deux tickets
     * @throws ResponseStatusException 404 si l'issue source ou cible est introuvable
     */
    public RelationDto createRelation(Long sourceId, RelationRequestDto dto, User user) {
        if (sourceId.equals(dto.getTargetIssueId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un ticket ne peut pas être lié à lui-même.");
        }

        Issue source = issueRepository.findById(sourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket source introuvable"));

        if (!issueFollowerRepository.existsByUserAndIssue(user, source)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous devez suivre ce ticket pour créer une liaison.");
        }

        boolean exists = relationRepository.existsBySourceIssueIdAndTargetIssueId(sourceId, dto.getTargetIssueId())
                || relationRepository.existsBySourceIssueIdAndTargetIssueId(dto.getTargetIssueId(), sourceId);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un lien existe déjà entre ces deux tickets.");
        }

        Issue target = issueRepository.findById(dto.getTargetIssueId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket cible introuvable"));

        IssueRelation relation = new IssueRelation();
        relation.setSourceIssue(source);
        relation.setTargetIssue(target);
        relation.setType(dto.getType());

        IssueRelation saved = relationRepository.save(relation);
        return mapToDto(saved);
    }

    /**
     * Récupère l'ensemble des relations impliquant un ticket, qu'il soit source ou cible.
     *
     * @param issueId identifiant du ticket concerné
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
     * Supprime définitivement un lien de relation entre deux tickets.
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