package fr.horizon.backend_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.HandleLinkRequestDto;
import fr.horizon.backend_api.model.dto.OfficialLinkRequestDto;
import fr.horizon.backend_api.model.dto.RelationDto;
import fr.horizon.backend_api.model.dto.RelationRequestDto;
import fr.horizon.backend_api.model.enums.RelationType;
import fr.horizon.backend_api.service.IssueFollowerService;
import fr.horizon.backend_api.service.IssueRelationService;
import fr.horizon.backend_api.service.JwtToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IssueRelationController.class)
@AutoConfigureMockMvc(addFilters = false)
class IssueRelationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IssueRelationService relationService;

    @MockitoBean
    private IssueFollowerService issueFollowerService;

    @MockitoBean
    private JwtToken jwtToken;

    @MockitoBean
    private Authentication authentication;

    /**
     * Test de succès : Création d'une liaison (Retourne 201 CREATED).
     */
    @Test
    void shouldLinkIssueAndReturnCreated() throws Exception {
        RelationRequestDto request = new RelationRequestDto();
        request.setTargetIssueId(2L);
        request.setType(RelationType.LINKED);

        RelationDto response = RelationDto.builder()
                .id(1L)
                .sourceIssueId(1L)
                .targetIssueId(2L)
                .type(RelationType.LINKED)
                .build();

        when(authentication.getName()).thenReturn("test@test.com");
        when(issueFollowerService.getUserByEmail(any())).thenReturn(new User());
        when(relationService.createRelation(eq(1L), any(RelationRequestDto.class), any(User.class)))
                .thenReturn(response);

        mockMvc.perform(post("/issueRelations/1/relations")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    /**
     * Test de succès alternatif : Une requête manager est générée (Retourne 202 ACCEPTED).
     */
    @Test
    void shouldLinkIssueAndReturnAcceptedWhenManagerRequestInitiated() throws Exception {
        RelationRequestDto request = new RelationRequestDto();
        request.setTargetIssueId(2L);
        request.setType(RelationType.DEPENDS_ON);

        when(authentication.getName()).thenReturn("test@test.com");
        when(issueFollowerService.getUserByEmail(any())).thenReturn(new User());
        // Le service retourne null quand un manager a fait une requête au second manager
        when(relationService.createRelation(eq(1L), any(RelationRequestDto.class), any(User.class)))
                .thenReturn(null);

        mockMvc.perform(post("/issueRelations/1/relations")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    /**
     * Test de gestion d'erreur lors de la création d'une liaison.
     */
    @Test
    void shouldReturn404WhenIssueMissing() throws Exception {
        RelationRequestDto request = new RelationRequestDto();
        request.setTargetIssueId(99L);
        request.setType(RelationType.LINKED);

        when(authentication.getName()).thenReturn("test@test.com");
        when(issueFollowerService.getUserByEmail(any())).thenReturn(new User());
        when(relationService.createRelation(eq(1L), any(RelationRequestDto.class), any(User.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket source introuvable"));

        mockMvc.perform(post("/issueRelations/1/relations")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    /**
     * Test de récupération des relations d'un ticket.
     */
    @Test
    void shouldGetRelations() throws Exception {
        RelationDto rel = RelationDto.builder()
                .id(1L)
                .type(RelationType.DEPENDS_ON)
                .build();

        when(relationService.getRelationsByIssueId(1L)).thenReturn(List.of(rel));

        mockMvc.perform(get("/issueRelations/1/relations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    /**
     * Test de suppression d'une liaison (Retourne 204 NO_CONTENT).
     */
    @Test
    void shouldUnlinkIssue() throws Exception {
        when(authentication.getName()).thenReturn("test@test.com");
        when(issueFollowerService.getUserByEmail(any())).thenReturn(new User());
        doNothing().when(relationService).deleteRelation(eq(1L), any(User.class));

        mockMvc.perform(delete("/issueRelations/relations/1")
                        .principal(authentication))
                .andExpect(status().isNoContent());

        verify(relationService).deleteRelation(eq(1L), any(User.class));
    }

    /**
     * Test de récupération des requêtes en attente pour un manager.
     */
    @Test
    void shouldGetPendingRequests() throws Exception {
        OfficialLinkRequestDto requestDto = OfficialLinkRequestDto.builder()
                .id(5L)
                .sourceIssueId(1L)
                .targetIssueId(2L)
                .type(RelationType.LINKED)
                .build();

        when(authentication.getName()).thenReturn("test@test.com");
        when(issueFollowerService.getUserByEmail(any())).thenReturn(new User());
        when(relationService.getPendingOfficialLinkRequests(any(User.class))).thenReturn(List.of(requestDto));

        mockMvc.perform(get("/issueRelations/relations/requests")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    /**
     * Test d'approbation d'une demande par les managers (Retourne 201 CREATED si finalisé).
     */
    @Test
    void shouldHandleLinkRequestAndReturnCreatedIfFinalized() throws Exception {
        HandleLinkRequestDto request = new HandleLinkRequestDto();
        request.setApprove(true);

        RelationDto finalRelation = RelationDto.builder()
                .id(10L)
                .manager(true)
                .build();

        when(authentication.getName()).thenReturn("test@test.com");
        when(issueFollowerService.getUserByEmail(any())).thenReturn(new User());
        // Simule le cas où les deux managers ont validé
        when(relationService.handleOfficialLinkRequest(eq(5L), any(HandleLinkRequestDto.class), any(User.class)))
                .thenReturn(finalRelation);

        mockMvc.perform(post("/issueRelations/relations/requests/5/handle")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    /**
     * Test d'approbation partielle ou de refus (Retourne 200 OK).
     */
    @Test
    void shouldHandleLinkRequestAndReturnOkIfPartialOrRejected() throws Exception {
        HandleLinkRequestDto request = new HandleLinkRequestDto();
        request.setApprove(false); // Cas d'un refus (ou d'une approbation en attente du 2ème manager)

        when(authentication.getName()).thenReturn("test@test.com");
        when(issueFollowerService.getUserByEmail(any())).thenReturn(new User());
        // Le service retourne null si le lien officiel n'est pas encore créé
        when(relationService.handleOfficialLinkRequest(eq(5L), any(HandleLinkRequestDto.class), any(User.class)))
                .thenReturn(null);

        mockMvc.perform(post("/issueRelations/relations/requests/5/handle")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}