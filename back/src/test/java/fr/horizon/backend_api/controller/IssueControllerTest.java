package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.IssueDto;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.service.IssueService;
import fr.horizon.backend_api.service.JwtToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IssueController.class)
@AutoConfigureMockMvc(addFilters = false)
class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssueService issueService;

    @MockitoBean
    private JwtToken jwtToken;

    @MockitoBean
    private Authentication authentication;

    @Test
    void shouldGetAllIssues() throws Exception {

        IssueDto dto = IssueDto.builder()
                .id(1L)
                .title("Bug")
                .description("Something is broken")
                .status(IssueStatus.OUVERT)
                .categoryId(1L)
                .categoryName("DEV")
                .createdId(1L)
                .createdName("John")
                .build();

        when(authentication.getName()).thenReturn("john@test.com");
        when(issueService.findAll("john@test.com")).thenReturn(List.of(dto));

        mockMvc.perform(get("/issues").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Bug"))
                .andExpect(jsonPath("$[0].categoryName").value("DEV"))
                .andExpect(jsonPath("$[0].createdName").value("John"));

        verify(issueService).findAll("john@test.com");
    }

    @Test
    void shouldCreateIssue() throws Exception {

        IssueDto response = IssueDto.builder()
                .id(1L)
                .title("Bug")
                .description("Something is broken")
                .status(IssueStatus.OUVERT)
                .categoryId(1L)
                .categoryName("DEV")
                .createdId(1L)
                .createdName("John")
                .build();

        when(issueService.createIssue(any(), anyString()))
                .thenReturn(response);

        when(authentication.getName()).thenReturn("john@test.com");

        mockMvc.perform(post("/issues")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "title": "Bug",
                      "description": "Something is broken",
                      "categoryId": 1
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Bug"))
                .andExpect(jsonPath("$.categoryName").value("DEV"));

        verify(issueService).createIssue(any(), eq("john@test.com"));
    }

    @Test
    void shouldDeleteIssue() throws Exception {

        mockMvc.perform(delete("/issues/1"))
                .andExpect(status().isOk());

        verify(issueService).deleteById(1L);
    }

    @Test
    void shouldGetIssueById() throws Exception {

        IssueDto dto = IssueDto.builder()
                .id(1L)
                .title("Bug")
                .description("desc")
                .build();

        when(authentication.getName()).thenReturn("john@test.com");
        when(issueService.findById(1L, "john@test.com")).thenReturn(dto);

        mockMvc.perform(get("/issues/1").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Bug"));

        verify(issueService).findById(1L, "john@test.com");
    }

    @Test
    void shouldUpdateIssueStatus() throws Exception {

        mockMvc.perform(post("/issues/1/update-status")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("TERMINE"))
                .andExpect(status().isOk());

        verify(issueService).updateStatus("TERMINE", 1L);
    }

    @Test
    void shouldToggleIssuePrivacy() throws Exception {

        when(authentication.getName()).thenReturn("john@test.com");

        mockMvc.perform(post("/issues/1/update-private").principal(authentication))
                .andExpect(status().isOk());

        verify(issueService).updatePrivacy(1L, "john@test.com");
    }
}
