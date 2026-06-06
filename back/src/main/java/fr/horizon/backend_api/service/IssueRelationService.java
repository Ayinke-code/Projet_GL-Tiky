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
 * Seuls les abonnés peuvent créer une liaison.
 * Les managers voient tous les liens, les étudiants voient uniquement les leurs et les liens officiels.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IssueRelationService {

    private final IssueRelationRepository relationRepository;
    private final IssueRepository issueRepository;
    private final IssueFollowerRepository issueFollowerRepository;

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

    /**
     * Crée une relation entre deux tickets distincts de la même catégorie.
     * L'utilisateur doit suivre le ticket source.
     * Un manager crée un lien officiel visible par tous.
     * Un étudiant crée un lien visible uniquement par lui et les managers.
     *
     * @param sourceId identifiant du ticket source
     * @param dto      contient l'identifiant cible et le type (LINKED, DEPENDS_ON)
     * @param user     utilisateur effectuant l'action
     * @return le DTO de la relation créée
     * @throws ResponseStatusException 400 si le ticket tente de se lier à lui-même
     * @throws ResponseStatusException 400 si les tickets ne sont pas de la même catégorie
     * @throws ResponseStatusException 403 si l'utilisateur ne suit pas le ticket source
     * @throws ResponseStatusException 404 si le ticket source ou cible est introuvable
     * @throws ResponseStatusException 409 si un lien identique existe déjà
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

        Issue target = issueRepository.findById(dto.getTargetIssueId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket cible introuvable"));

        if (source.getCategory() == null || target.getCategory() == null
                || !source.getCategory().getId().equals(target.getCategory().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les deux tickets doivent appartenir à la même catégorie.");
        }

        boolean manager = isManager(source, target, user);

        // Un manager ne peut pas créer deux liens officiels identiques
        if (manager) {
            boolean officialExists =
                    relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(sourceId, dto.getTargetIssueId())
                            || relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(dto.getTargetIssueId(), sourceId);
            if (officialExists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Un lien officiel existe déjà entre ces deux tickets.");
            }
        }

        // Tout utilisateur (manager ou non) ne peut pas recréer un lien qu'il a déjà créé
        boolean alreadyLinkedByUser =
                relationRepository.existsBySourceIssueIdAndTargetIssueIdAndCreatedByAndManagerFalse(sourceId, dto.getTargetIssueId(), user)
                        || relationRepository.existsBySourceIssueIdAndTargetIssueIdAndCreatedByAndManagerFalse(dto.getTargetIssueId(), sourceId, user);
        if (alreadyLinkedByUser) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous avez déjà créé ce lien.");
        }

        // Bloquer si un lien officiel identique existe déjà
        boolean officialExists =
                relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(sourceId, dto.getTargetIssueId())
                        || relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(dto.getTargetIssueId(), sourceId);
        if (officialExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un lien officiel existe déjà entre ces deux tickets.");
        }

        IssueRelation relation = new IssueRelation();
        relation.setSourceIssue(source);
        relation.setTargetIssue(target);
        relation.setType(dto.getType());
        relation.setManager(manager);
        relation.setCreatedBy(user);

        IssueRelation saved = relationRepository.save(relation);
        return mapToDto(saved);
    }

    /**
     * Récupère toutes les relations d'un ticket sans filtrage.
     * Le filtrage par rôle est géré côté front via createdById et manager.
     *
     * @param issueId identifiant du ticket
     * @return liste des DTOs de relations
     */
    @Transactional(readOnly = true)
    public List<RelationDto> getRelationsByIssueId(Long issueId) {
        return relationRepository.findAllBySourceIssueIdOrTargetIssueId(issueId, issueId)
                .stream()
                .map(this::mapToDto)
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
}