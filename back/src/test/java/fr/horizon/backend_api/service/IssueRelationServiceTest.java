package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.*;
import fr.horizon.backend_api.model.dto.HandleLinkRequestDto;
import fr.horizon.backend_api.model.dto.RelationDto;
import fr.horizon.backend_api.model.dto.RelationRequestDto;
import fr.horizon.backend_api.model.enums.LinkRejectionReason;
import fr.horizon.backend_api.model.enums.RelationType;
import fr.horizon.backend_api.repository.IssueFollowerRepository;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.IssueRelationRepository;
import fr.horizon.backend_api.repository.OfficialLinkRequestRepository;
import org.junit.jupiter.api.BeforeEach;
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
 */
@ExtendWith(MockitoExtension.class)
class IssueRelationServiceTest {

    @Mock
    private IssueRelationRepository relationRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private IssueFollowerRepository issueFollowerRepository;

    @Mock
    private OfficialLinkRequestRepository linkRequestRepository;

    @InjectMocks
    private IssueRelationService relationService;

    private User user;
    private Category category;
    private Issue sourceIssue;
    private Issue targetIssue;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(100L);

        category = new Category();
        category.setId(1L);

        sourceIssue = new Issue();
        sourceIssue.setId(10L);
        sourceIssue.setTitle("Source");
        sourceIssue.setCategory(category);

        targetIssue = new Issue();
        targetIssue.setId(11L);
        targetIssue.setTitle("Cible");
        targetIssue.setCategory(category);
    }

    /**
     * Scénario nominal de création d'une liaison par un étudiant.
     */
    @Test
    void createRelation_success() {
        RelationRequestDto dto = new RelationRequestDto();
        dto.setTargetIssueId(targetIssue.getId());
        dto.setType(RelationType.DEPENDS_ON);

        IssueRelation savedRelation = new IssueRelation();
        savedRelation.setId(1L);
        savedRelation.setSourceIssue(sourceIssue);
        savedRelation.setTargetIssue(targetIssue);
        savedRelation.setType(RelationType.DEPENDS_ON);
        savedRelation.setCreatedBy(user);

        when(issueRepository.findById(10L)).thenReturn(Optional.of(sourceIssue));
        when(issueRepository.findById(11L)).thenReturn(Optional.of(targetIssue));

        when(relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(10L, 11L)).thenReturn(false);
        when(relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(11L, 10L)).thenReturn(false);

        when(issueFollowerRepository.existsByUserAndIssue(user, sourceIssue)).thenReturn(true);

        when(relationRepository.existsBySourceIssueIdAndTargetIssueIdAndCreatedByAndManagerFalse(10L, 11L, user)).thenReturn(false);
        when(relationRepository.existsBySourceIssueIdAndTargetIssueIdAndCreatedByAndManagerFalse(11L, 10L, user)).thenReturn(false);

        when(relationRepository.saveAndFlush(any(IssueRelation.class))).thenReturn(savedRelation);

        when(relationRepository.countNonManagerRelationsByType(10L, 11L, RelationType.DEPENDS_ON)).thenReturn(1);

        RelationDto result = relationService.createRelation(10L, dto, user);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(relationRepository).saveAndFlush(any(IssueRelation.class));
    }

    /**
     * Règle métier : Interdiction de lier un ticket à lui-même.
     */
    @Test
    void createRelation_selfLink_throwsBadRequest() {
        RelationRequestDto dto = new RelationRequestDto();
        dto.setTargetIssueId(10L);

        when(issueRepository.findById(10L)).thenReturn(Optional.of(sourceIssue));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                relationService.createRelation(10L, dto, user));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    /**
     * Règle métier : Restriction aux abonnés.
     */
    @Test
    void createRelation_notFollower_throwsForbidden() {
        RelationRequestDto dto = new RelationRequestDto();
        dto.setTargetIssueId(11L);
        dto.setType(RelationType.DEPENDS_ON);

        when(issueRepository.findById(10L)).thenReturn(Optional.of(sourceIssue));
        when(issueRepository.findById(11L)).thenReturn(Optional.of(targetIssue));

        when(relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(10L, 11L)).thenReturn(false);
        when(relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(11L, 10L)).thenReturn(false);

        when(issueFollowerRepository.existsByUserAndIssue(user, sourceIssue)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                relationService.createRelation(10L, dto, user));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    /**
     * Règle métier : Unicité des relations pour un étudiant.
     */
    @Test
    void createRelation_alreadyExists_throwsConflict() {
        RelationRequestDto dto = new RelationRequestDto();
        dto.setTargetIssueId(11L);
        dto.setType(RelationType.DEPENDS_ON);

        when(issueRepository.findById(10L)).thenReturn(Optional.of(sourceIssue));
        when(issueRepository.findById(11L)).thenReturn(Optional.of(targetIssue));

        when(relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(10L, 11L)).thenReturn(false);
        when(relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(11L, 10L)).thenReturn(false);

        when(issueFollowerRepository.existsByUserAndIssue(user, sourceIssue)).thenReturn(true);
        when(relationRepository.existsBySourceIssueIdAndTargetIssueIdAndCreatedByAndManagerFalse(10L, 11L, user)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () ->
                relationService.createRelation(10L, dto, user));
    }

    /**
     * Règle métier : Intégrité référentielle.
     */
    @Test
    void createRelation_issueNotFound_throwsNotFound() {
        RelationRequestDto dto = new RelationRequestDto();
        dto.setTargetIssueId(11L);
        dto.setType(RelationType.LINKED);

        when(issueRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                relationService.createRelation(10L, dto, user));
    }

    /**
     * Scénario nominal de suppression.
     */
    @Test
    void deleteRelation_success() {
        IssueRelation relation = new IssueRelation();
        relation.setId(1L);
        relation.setCreatedBy(user);
        relation.setSourceIssue(sourceIssue);
        relation.setTargetIssue(targetIssue);

        when(relationRepository.findById(1L)).thenReturn(Optional.of(relation));

        assertDoesNotThrow(() -> relationService.deleteRelation(1L, user));
        verify(relationRepository).deleteById(1L);
    }

    /**
     * Scénario alternatif de suppression.
     */
    @Test
    void deleteRelation_notFound_throwsNotFound() {
        when(relationRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                relationService.deleteRelation(99L, user));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    /**
     * Interdiction de supprimer le lien d'un autre (sans être manager).
     */
    @Test
    void deleteRelation_forbidden_throwsForbidden() {
        User otherUser = new User();
        otherUser.setId(99L);

        IssueRelation relation = new IssueRelation();
        relation.setId(1L);
        relation.setCreatedBy(user);
        relation.setSourceIssue(sourceIssue);
        relation.setTargetIssue(targetIssue);

        when(relationRepository.findById(1L)).thenReturn(Optional.of(relation));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                relationService.deleteRelation(1L, otherUser));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void createRelation_differentCategories_throwsBadRequest() {
        Category otherCategory = new Category();
        otherCategory.setId(2L);

        targetIssue.setCategory(otherCategory);

        RelationRequestDto dto = new RelationRequestDto();
        dto.setTargetIssueId(11L);
        dto.setType(RelationType.DEPENDS_ON);

        when(issueRepository.findById(10L)).thenReturn(Optional.of(sourceIssue));
        when(issueRepository.findById(11L)).thenReturn(Optional.of(targetIssue));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> relationService.createRelation(10L, dto, user)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void createRelation_officialLinkAlreadyExists_throwsConflict() {
        RelationRequestDto dto = new RelationRequestDto();
        dto.setTargetIssueId(11L);
        dto.setType(RelationType.DEPENDS_ON);

        when(issueRepository.findById(10L)).thenReturn(Optional.of(sourceIssue));
        when(issueRepository.findById(11L)).thenReturn(Optional.of(targetIssue));

        when(relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(10L, 11L))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> relationService.createRelation(10L, dto, user)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void createRelation_thresholdReached_createsOfficialRequest() {
        RelationRequestDto dto = new RelationRequestDto();
        dto.setTargetIssueId(11L);
        dto.setType(RelationType.DEPENDS_ON);

        IssueRelation relation = new IssueRelation();
        relation.setId(1L);
        relation.setSourceIssue(sourceIssue);
        relation.setTargetIssue(targetIssue);
        relation.setCreatedBy(user);
        relation.setType(RelationType.DEPENDS_ON);

        when(issueRepository.findById(10L)).thenReturn(Optional.of(sourceIssue));
        when(issueRepository.findById(11L)).thenReturn(Optional.of(targetIssue));

        when(relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(anyLong(), anyLong()))
                .thenReturn(false);

        when(issueFollowerRepository.existsByUserAndIssue(user, sourceIssue))
                .thenReturn(true);

        when(relationRepository.existsBySourceIssueIdAndTargetIssueIdAndCreatedByAndManagerFalse(anyLong(), anyLong(), eq(user)))
                .thenReturn(false);

        when(relationRepository.saveAndFlush(any())).thenReturn(relation);

        when(relationRepository.countNonManagerRelationsByType(
                10L, 11L, RelationType.DEPENDS_ON))
                .thenReturn(5);

        when(linkRequestRepository.existsBySourceIssueIdAndTargetIssueIdAndTypeAndIsHandledFalse(
                anyLong(), anyLong(), any()))
                .thenReturn(false);

        relationService.createRelation(10L, dto, user);

        verify(linkRequestRepository).save(any(OfficialLinkRequest.class));
    }

    @Test
    void deleteRelation_managerCanDelete() {
        User manager = new User();
        manager.setId(200L);

        sourceIssue.setManager(manager);

        IssueRelation relation = new IssueRelation();
        relation.setId(1L);
        relation.setCreatedBy(user);
        relation.setSourceIssue(sourceIssue);
        relation.setTargetIssue(targetIssue);

        when(relationRepository.findById(1L))
                .thenReturn(Optional.of(relation));

        assertDoesNotThrow(() ->
                relationService.deleteRelation(1L, manager));

        verify(relationRepository).deleteById(1L);
    }

    @Test
    void createRelation_managerOfOneIssue_createsRequest() {
        User manager = new User();
        manager.setId(200L);

        sourceIssue.setManager(manager);

        RelationRequestDto dto = new RelationRequestDto();
        dto.setTargetIssueId(11L);
        dto.setType(RelationType.DEPENDS_ON);

        when(issueRepository.findById(10L)).thenReturn(Optional.of(sourceIssue));
        when(issueRepository.findById(11L)).thenReturn(Optional.of(targetIssue));

        when(relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(anyLong(), anyLong()))
                .thenReturn(false);

        RelationDto result = relationService.createRelation(10L, dto, manager);

        assertNull(result);

        verify(linkRequestRepository).save(any(OfficialLinkRequest.class));
        verify(relationRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRelation_managerOfBothIssues_createsOfficialRelation() {
        User manager = new User();
        manager.setId(200L);

        sourceIssue.setManager(manager);
        targetIssue.setManager(manager);

        RelationRequestDto dto = new RelationRequestDto();
        dto.setTargetIssueId(11L);
        dto.setType(RelationType.DEPENDS_ON);

        IssueRelation relation = new IssueRelation();
        relation.setId(1L);
        relation.setSourceIssue(sourceIssue);
        relation.setTargetIssue(targetIssue);
        relation.setCreatedBy(manager);
        relation.setManager(true);

        when(issueRepository.findById(10L)).thenReturn(Optional.of(sourceIssue));
        when(issueRepository.findById(11L)).thenReturn(Optional.of(targetIssue));

        when(relationRepository.existsBySourceIssueIdAndTargetIssueIdAndManagerTrue(anyLong(), anyLong()))
                .thenReturn(false);

        when(relationRepository.existsBySourceIssueIdAndTargetIssueIdAndCreatedByAndManagerFalse(anyLong(), anyLong(), eq(manager)))
                .thenReturn(false);

        when(relationRepository.saveAndFlush(any())).thenReturn(relation);

        RelationDto dtoResult = relationService.createRelation(10L, dto, manager);

        assertNotNull(dtoResult);
        verify(relationRepository).saveAndFlush(any(IssueRelation.class));
    }

    @Test
    void handleOfficialLinkRequest_reject_success() {
        User manager = new User();
        manager.setId(200L);

        sourceIssue.setManager(manager);

        OfficialLinkRequest request = OfficialLinkRequest.builder()
                .id(1L)
                .sourceIssue(sourceIssue)
                .targetIssue(targetIssue)
                .isHandled(false)
                .build();

        HandleLinkRequestDto dto = new HandleLinkRequestDto();
        dto.setApprove(false);
        dto.setRejectionReason(LinkRejectionReason.WRONG_RELATION_TYPE);

        when(linkRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        assertDoesNotThrow(() ->
                relationService.handleOfficialLinkRequest(1L, dto, manager));

        verify(linkRequestRepository).save(request);
    }

    @Test
    void handleOfficialLinkRequest_bothApproved_createsOfficialRelation() {
        User manager = new User();
        manager.setId(200L);

        sourceIssue.setManager(manager);
        targetIssue.setManager(manager);

        OfficialLinkRequest request = OfficialLinkRequest.builder()
                .id(1L)
                .sourceIssue(sourceIssue)
                .targetIssue(targetIssue)
                .type(RelationType.DEPENDS_ON)
                .isHandled(false)
                .build();

        HandleLinkRequestDto dto = new HandleLinkRequestDto();
        dto.setApprove(true);

        IssueRelation savedRelation = new IssueRelation();
        savedRelation.setId(99L);
        savedRelation.setSourceIssue(sourceIssue);
        savedRelation.setTargetIssue(targetIssue);
        savedRelation.setCreatedBy(manager);

        when(linkRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        when(relationRepository.save(any(IssueRelation.class)))
                .thenReturn(savedRelation);

        RelationDto result =
                relationService.handleOfficialLinkRequest(1L, dto, manager);

        assertNotNull(result);
        verify(relationRepository).save(any(IssueRelation.class));
    }




}