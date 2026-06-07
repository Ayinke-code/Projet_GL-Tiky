package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.dto.RelationDto;
import fr.horizon.backend_api.model.dto.RelationRequestDto;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.IssueRelation;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.RelationStatus;
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
 * Pour un lien entre deux issues de managers différents, les deux managers doivent valider.
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
     * Si le créateur est manager :
     *   - même manager pour les deux tickets → lien directement OFFICIAL
     *   - managers différents → lien PENDING, le second manager doit valider
     * Un étudiant ne peut pas créer un lien si un lien officiel (OFFICIAL) existe déjà.
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

        // Bloquer si un lien officiel validé existe déjà entre ces deux tickets
        boolean officialExists =
                relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrueAndStatus(sourceId, dto.getTargetIssueId(), RelationStatus.OFFICIAL)
                        || relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrueAndStatus(dto.getTargetIssueId(), sourceId, RelationStatus.OFFICIAL);
        if (officialExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un lien officiel existe déjà entre ces deux tickets.");
        }

        boolean manager = isManager(source, target, user);

        // Tout utilisateur ne peut pas recréer un lien qu'il a déjà créé
        boolean alreadyLinkedByUser =
                relationRepository.existsBySourceIssueIdAndTargetIssueIdAndCreatedByAndManagerFalse(sourceId, dto.getTargetIssueId(), user)
                        || relationRepository.existsBySourceIssueIdAndTargetIssueIdAndCreatedByAndManagerFalse(dto.getTargetIssueId(), sourceId, user);
        if (alreadyLinkedByUser) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous avez déjà créé ce lien.");
        }

        IssueRelation relation = new IssueRelation();
        relation.setSourceIssue(source);
        relation.setTargetIssue(target);
        relation.setType(dto.getType());
        relation.setCreatedBy(user);

        if (manager) {
            relation.setManager(true);

            User sourceManager = source.getManager();
            User targetManager = target.getManager();

            // Même manager pour les deux tickets → lien directement officiel
            boolean sameManager = sourceManager != null && targetManager != null
                    && sourceManager.getId().equals(targetManager.getId());

            if (sameManager) {
                relation.setStatus(RelationStatus.OFFICIAL);
                relation.setValidator(null);
            } else {
                // Managers différents → PENDING, le second manager doit valider
                relation.setStatus(RelationStatus.PENDING);
                // Le validateur est l'autre manager (celui qui n'a pas créé le lien)
                User validator = sourceManager != null && sourceManager.getId().equals(user.getId())
                        ? targetManager
                        : sourceManager;
                relation.setValidator(validator);
            }
        } else {
            relation.setManager(false);
            relation.setStatus(RelationStatus.PENDING);
            relation.setValidator(null);
        }

        IssueRelation saved = relationRepository.save(relation);
        return mapToDto(saved);
    }

    /**
     * Permet au second manager de valider ou refuser un lien en attente.
     *
     * @param relationId identifiant de la relation à valider
     * @param accept     true pour valider, false pour refuser
     * @param user       manager effectuant la validation
     * @return le DTO de la relation mise à jour
     * @throws ResponseStatusException 403 si l'utilisateur n'est pas le validateur désigné
     * @throws ResponseStatusException 404 si la relation n'existe pas
     * @throws ResponseStatusException 409 si la relation n'est pas en statut PENDING
     */
    public RelationDto validateRelation(Long relationId, boolean accept, User user) {
        IssueRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relation introuvable"));

        if (relation.getStatus() != RelationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce lien n'est pas en attente de validation.");
        }

        if (relation.getValidator() == null || !relation.getValidator().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas le validateur désigné pour ce lien.");
        }

        relation.setStatus(accept ? RelationStatus.OFFICIAL : RelationStatus.REJECTED);
        IssueRelation saved = relationRepository.save(relation);
        return mapToDto(saved);
    }

    /**
     * Récupère tous les liens en attente de validation pour le manager connecté.
     *
     * @param user manager connecté
     * @return liste des DTOs de relations en attente
     */
    @Transactional(readOnly = true)
    public List<RelationDto> getPendingRelationsForValidator(User user) {
        return relationRepository.findAllByValidatorAndStatus(user, RelationStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Récupère toutes les relations d'un ticket sans filtrage.
     * Le filtrage par rôle est géré côté front via createdById, manager et status.
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
     * @param user       utilisateur effectuant la suppression
     * @throws ResponseStatusException 403 si l'utilisateur n'est pas créateur ou manager
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
        dto.setStatus(relation.getStatus());
        dto.setValidatorId(relation.getValidator() != null ? relation.getValidator().getId() : null);
        return dto;
    }
}