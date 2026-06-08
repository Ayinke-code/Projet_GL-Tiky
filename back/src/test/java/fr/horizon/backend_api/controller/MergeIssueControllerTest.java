package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.MergeRequestDto;
import fr.horizon.backend_api.model.enums.MergeRequestStatus;
import fr.horizon.backend_api.service.MergeIssueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MergeIssueControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MergeIssueService mergeIssueService;

    @InjectMocks
    private MergeIssueController mergeIssueController;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mergeIssueController).build();
        // Simuler un utilisateur connecté
        authentication = new UsernamePasswordAuthenticationToken("test@horizon.fr", "password");
    }

    @Test
    void requestMerge_ShouldReturn201() throws Exception {
        MergeRequestDto mockDto = new MergeRequestDto();
        mockDto.setId(1L);
        mockDto.setSourceIssueId(10L);
        mockDto.setTargetIssueId(20L);
        mockDto.setStatus(MergeRequestStatus.PENDING);

        when(mergeIssueService.sendMergeRequest(eq(10L), eq(20L), eq("test@horizon.fr")))
                .thenReturn(mockDto);

        mockMvc.perform(post("/merging/10/merge")
                        .param("targetId", "20")
                        .principal(authentication))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void respondToMergeRequest_ShouldReturn200() throws Exception {
        MergeRequestDto mockDto = new MergeRequestDto();
        mockDto.setId(100L);
        mockDto.setStatus(MergeRequestStatus.ACCEPTED);

        when(mergeIssueService.respondToMergeRequest(eq(100L), eq(true), isNull(), eq("test@horizon.fr")))
                .thenReturn(mockDto);

        mockMvc.perform(post("/merging/100/respond")
                        .param("accept", "true")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void getPendingMergeRequests_ShouldReturnListOfRequests() throws Exception {
        MergeRequestDto mockDto = new MergeRequestDto();
        mockDto.setId(5L);
        when(mergeIssueService.getManagerRequestDto("test@horizon.fr")).thenReturn(List.of(mockDto));

        mockMvc.perform(get("/merging/requests/pending")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void getDeniedMergeRequests_ShouldReturnListOfRequests() throws Exception {
        MergeRequestDto mockDto = new MergeRequestDto();
        mockDto.setId(6L);
        when(mergeIssueService.getRecentDeniedRequests("test@horizon.fr")).thenReturn(List.of(mockDto));

        mockMvc.perform(get("/merging/requests/denied")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(6));
    }
}