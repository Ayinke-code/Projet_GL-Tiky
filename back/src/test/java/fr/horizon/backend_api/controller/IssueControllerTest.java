package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.IssueDTO;
import fr.horizon.backend_api.model.dto.UpdateIssueStatusDTO;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.repository.UserRepository;
import fr.horizon.backend_api.service.IssueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueControllerTest {

    @Mock
    private IssueService issueService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private IssueController issueController;

    @Test
    @DisplayName("createIssue devrait retourner une issue créée")
    void createIssue_Success() {
        IssueDTO dto = IssueDTO.builder()
                .titre("Incident réseau")
                .description("Le wifi ne fonctionne plus")
                .categorieId(1L)
                .build();

        Category category = Category.builder()
                .id(1L)
                .name("Réseau")
                .subject("Informatique")
                .categoryOwner("teacher-1")
                .build();

        User creator = User.builder()
                .id(10L)
                .email("alice@example.com")
                .name("Alice")
                .build();

        Issue issue = Issue.builder()
                .idTick(99L)
                .titre(dto.getTitre())
                .description(dto.getDescription())
                .statut(IssueStatus.OUVERT)
                .categorie(category)
                .createur(creator)
                .build();

        when(authentication.getName()).thenReturn("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(creator));
        when(issueService.createIssue(any(), any())).thenReturn(issue);

        var response = issueController.createIssue(dto, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(99L, response.getBody().getIdTick());
        verify(issueService).createIssue(any(), eq(creator));
    }

    @Test
    @DisplayName("createIssue devrait échouer si l'utilisateur est introuvable")
    void createIssue_UserNotFound() {
        IssueDTO dto = IssueDTO.builder()
                .titre("Incident réseau")
                .description("Le wifi ne fonctionne plus")
                .categorieId(1L)
                .build();

        when(authentication.getName()).thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                issueController.createIssue(dto, authentication));

        assertEquals("Utilisateur non trouvé", exception.getMessage());
    }

    @Test
    @DisplayName("getAllIssues devrait retourner toutes les issues")
    void getAllIssues_Success() {
        Issue issue = Issue.builder().idTick(1L).titre("Incident réseau").build();
        when(issueService.getAllIssues()).thenReturn(List.of(issue));

        var response = issueController.getAllIssues();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("getIssueById devrait retourner une issue")
    void getIssueById_Success() {
        Issue issue = Issue.builder().idTick(5L).titre("Ticket critique").build();
        when(issueService.getIssueById(5L)).thenReturn(issue);

        var response = issueController.getIssueById(5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5L, response.getBody().getIdTick());
    }

    @Test
    @DisplayName("getIssuesByCategory devrait retourner les issues d'une catégorie")
    void getIssuesByCategory_Success() {
        Issue issue = Issue.builder().idTick(7L).titre("Ticket catégorie").build();
        when(issueService.getIssuesByCategory(3L)).thenReturn(List.of(issue));

        var response = issueController.getIssuesByCategory(3L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(7L, response.getBody().get(0).getIdTick());
    }

    @Test
    @DisplayName("getIssuesByStatus devrait retourner les issues filtrées")
    void getIssuesByStatus_Success() {
        Issue issue = Issue.builder().idTick(8L).titre("Ticket ouvert").build();
        when(issueService.getIssuesByStatus("OUVERT")).thenReturn(List.of(issue));

        var response = issueController.getIssuesByStatus("OUVERT");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(8L, response.getBody().get(0).getIdTick());
    }

    @Test
    @DisplayName("updateIssueStatus devrait mettre à jour le statut")
    void updateIssueStatus_Success() {
        Issue updatedIssue = Issue.builder().idTick(9L).statut(IssueStatus.COMPLET).build();
        UpdateIssueStatusDTO dto = new UpdateIssueStatusDTO();
        dto.setStatut("COMPLET");

        when(issueService.updateIssueStatus(eq(9L), any())).thenReturn(updatedIssue);

        var response = issueController.updateIssueStatus(9L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(IssueStatus.COMPLET, response.getBody().getStatut());
    }

    @Test
    @DisplayName("deleteIssue devrait supprimer une issue")
    void deleteIssue_Success() {
        var response = issueController.deleteIssue(12L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(issueService).deleteIssue(12L);
    }
}
