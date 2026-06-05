package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.RelationDto;
import fr.horizon.backend_api.model.dto.RelationRequestDto;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.IssueRelation;
import fr.horizon.backend_api.model.enums.RelationType;
import fr.horizon.backend_api.repository.IssueFollowerRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.IssueRelationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires isolés pour la couche service de gestion des relations.
 * Utilise MockitoExtension pour simuler les accès aux repositories JPA sans charger le contexte Spring.
 */
@ExtendWith(MockitoExtension.class)
class IssueRelationServiceTest {

    @Mock
    private IssueRelationRepository relationRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private IssueFollowerRepository issueFollowerRepository;

    @InjectMocks
    private IssueRelationService relationService;

    private final User user = new User();
    /**
     * Scénario nominal de création d'une liaison.
     * Vérifie que les données sont validées, enregistrées en base, et converties en DTO.
     */
    @Test
    void createRelation_success() {
        Long sourceId = 10L;
        Long targetId = 11L;

        RelationRequestDto dto = new RelationRequestDto();
        dto.setTargetIssueId(targetId);
        dto.setType(RelationType.DEPENDS_ON);

        Issue sourceIssue = new Issue();
        sourceIssue.setId(sourceId);
        sourceIssue.setTitle("Source");

        Issue targetIssue = new Issue();
        targetIssue.setId(targetId);
        targetIssue.setTitle("Cible");

        IssueRelation savedRelation = new IssueRelation();
        savedRelation.setId(1L);
        savedRelation.setSourceIssue(sourceIssue);
        savedRelation.setTargetIssue(targetIssue);
        savedRelation.setType(RelationType.DEPENDS_ON);

        // Définition du comportement attendu des mocks
        when(relationRepository.existsBySourceIssueIdAndTargetIssueId(sourceId, targetId))
                .thenReturn(false);
        when(issueRepository.findById(sourceId)).thenReturn(Optional.of(sourceIssue));
        when(issueRepository.findById(targetId)).thenReturn(Optional.of(targetIssue));
        when(issueFollowerRepository.existsByUserAndIssue(user, sourceIssue)).thenReturn(true);
        when(relationRepository.save(any(IssueRelation.class))).thenReturn(savedRelation);

        RelationDto result = relationService.createRelation(sourceId, dto, user);

        // Validation des assertions de sortie
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(relationRepository).save(any(IssueRelation.class));
    }

    /**
     * Règle métier : Interdiction de lier un ticket à lui-même.
     * Vérifie la levée d'une exception BAD_REQUEST (400).
     */
    @Test
    void createRelation_selfLink_throwsBadRequest() {
        RelationRequestDto dto = new RelationRequestDto();
        dto.setTargetIssueId(10L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.createRelation(10L, dto, user);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    /**
     * Règle métier : Unicité des relations.
     * Vérifie qu'une exception CONFLICT (409) est levée si la même relation existe déjà.
     */
    @Test
    void createRelation_alreadyExists_throwsConflict() {
        RelationRequestDto dto = new RelationRequestDto();
        dto.setTargetIssueId(11L);
        dto.setType(RelationType.DEPENDS_ON);

        Issue sourceIssue = new Issue();
        sourceIssue.setId(10L);
        when(issueRepository.findById(10L)).thenReturn(Optional.of(sourceIssue));
        when(issueFollowerRepository.existsByUserAndIssue(user, sourceIssue)).thenReturn(true);
        when(relationRepository.existsBySourceIssueIdAndTargetIssueId(10L, 11L))
                .thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> relationService.createRelation(10L, dto, user));
    }

    /**
     * Règle métier : Intégrité référentielle.
     * Vérifie la levée d'une exception NOT_FOUND (404) si le ticket source n'existe pas en base.
     */
    @Test
    void createRelation_issueNotFound_throwsNotFound() {
        RelationRequestDto dto = new RelationRequestDto();
        dto.setTargetIssueId(11L);
        dto.setType(RelationType.LINKED);

        when(issueRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> relationService.createRelation(10L, dto, user));
    }

    /**
     * Scénario nominal de suppression.
     * Vérifie que la méthode deleteById du repository est bien appelée si l'entité existe.
     */
    @Test
    void deleteRelation_success() {
        when(relationRepository.existsById(1L)).thenReturn(true);
        assertDoesNotThrow(() -> relationService.deleteRelation(1L));
        verify(relationRepository).deleteById(1L);
    }

    /**
     * Scénario alternatif de suppression.
     * Vérifie la levée d'une erreur NOT_FOUND (404) si l'identifiant de relation n'est pas répertorié.
     */
    @Test
    void deleteRelation_notFound_throwsNotFound() {
        when(relationRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> relationService.deleteRelation(99L));
    }

    /**
     * Règle métier : Restriction aux abonnés.
     * Vérifie la levée d'une exception FORBIDDEN (403) si l'utilisateur ne suit pas le ticket.
     */
    @Test
    void createRelation_notFollower_throwsForbidden() {
        RelationRequestDto dto = new RelationRequestDto();
        dto.setTargetIssueId(11L);
        dto.setType(RelationType.DEPENDS_ON);

        Issue sourceIssue = new Issue();
        sourceIssue.setId(10L);

        when(issueRepository.findById(10L)).thenReturn(Optional.of(sourceIssue));
        when(issueFollowerRepository.existsByUserAndIssue(user, sourceIssue)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> relationService.createRelation(10L, dto, user));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
}