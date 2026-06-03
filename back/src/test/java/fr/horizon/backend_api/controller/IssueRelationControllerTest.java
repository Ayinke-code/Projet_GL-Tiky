package fr.horizon.backend_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.horizon.backend_api.model.dto.RelationDto;
import fr.horizon.backend_api.model.dto.RelationRequestDto;
import fr.horizon.backend_api.model.enums.RelationType;
import fr.horizon.backend_api.service.IssueRelationService;
import fr.horizon.backend_api.service.JwtToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour le contrôleur des relations entre issues.
 * L'option addFilters = false désactive la sécurité globale pour tester uniquement la logique de routage.
 */
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
    private JwtToken jwtToken;

    /**
     * Test de succès pour la création d'une liaison.
     * Vérifie que l'endpoint POST retourne un statut 200 et le DTO correspondant.
     */
    @Test
    void shouldLinkIssue() throws Exception {
        // Préparation de la requête de test
        RelationRequestDto request = new RelationRequestDto();
        request.setTargetIssueId(2L);
        request.setType(RelationType.LINKED);

        // Simulation de la réponse du service
        RelationDto response = RelationDto.builder()
                .id(1L)
                .sourceIssueId(1L)
                .targetIssueId(2L)
                .type(RelationType.LINKED)
                .build();

        when(relationService.createRelation(eq(1L), any(RelationRequestDto.class)))
                .thenReturn(response);

        // Exécution de la requête HTTP et vérification des assertions
        mockMvc.perform(post("/issues/1/relations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    /**
     * Test de gestion d'erreur lors de la création d'une liaison.
     * Vérifie qu'un statut 404 est retourné si l'issue cible ou source n'est pas trouvée.
     */
    @Test
    void shouldReturn404WhenIssueMissing() throws Exception {
        RelationRequestDto request = new RelationRequestDto();
        request.setTargetIssueId(99L);
        request.setType(RelationType.LINKED);

        // Simulation d'une levée d'exception par le service métier
        when(relationService.createRelation(eq(1L), any(RelationRequestDto.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue source introuvable"));

        mockMvc.perform(post("/issues/1/relations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    /**
     * Test de récupération des relations d'un ticket.
     * Vérifie que l'endpoint GET retourne un statut 200 et une liste JSON valide.
     */
    @Test
    void shouldGetRelations() throws Exception {
        RelationDto rel = RelationDto.builder()
                .id(1L)
                .type(RelationType.DEPENDS_ON)
                .build();

        when(relationService.getRelationsByIssueId(1L)).thenReturn(List.of(rel));

        mockMvc.perform(get("/issues/1/relations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    /**
     * Test de suppression d'une liaison.
     * Vérifie que l'endpoint DELETE invoque correctement le service et retourne un statut 200.
     */
    @Test
    void shouldUnlinkIssue() throws Exception {
        doNothing().when(relationService).deleteRelation(1L);

        mockMvc.perform(delete("/issues/relations/1"))
                .andExpect(status().isOk());

        verify(relationService).deleteRelation(1L);
    }
}