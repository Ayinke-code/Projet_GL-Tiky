package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.EventResponseDto;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.service.EventService;
import fr.horizon.backend_api.service.JwtToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private JwtToken jwtToken;

    @MockitoBean
    private Authentication authentication;


    @Test
    void shouldCreateEvent_returns201() throws Exception {

        EventResponseDto response = EventResponseDto.builder()
                .id(1L)
                .name("Audit Sprint 3")
                .description("Revue du code du sprint 3")
                .categoryType(CategoryType.AUDIT)
                .categoryName("UI/UX")
                .createdBy("Sophie ROUSSEAU")
                .createdAt(LocalDateTime.now())
                .build();

        when(eventService.create(any(), anyString())).thenReturn(response);
        when(authentication.getName()).thenReturn("teacher@teacher.fr");

        mockMvc.perform(post("/events")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 10,
                                  "name": "Audit Sprint 3",
                                  "description": "Revue du code du sprint 3"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Audit Sprint 3"))
                .andExpect(jsonPath("$.categoryType").value("AUDIT"))
                .andExpect(jsonPath("$.categoryName").value("UI/UX"))
                .andExpect(jsonPath("$.createdBy").value("Sophie ROUSSEAU"));

        verify(eventService).create(any(), anyString());
    }

    @Test
    void shouldCreateEvent_defense_returns201() throws Exception {

        EventResponseDto response = EventResponseDto.builder()
                .id(2L)
                .name("Soutenance PFE - Groupe A")
                .description("Présentation du projet de fin d'études")
                .categoryType(CategoryType.DEFENSE)
                .categoryName("PFE")
                .createdBy("Richard WOODWARD")
                .createdAt(LocalDateTime.now())
                .build();

        when(eventService.create(any(), anyString())).thenReturn(response);
        when(authentication.getName()).thenReturn("admin@admin.fr");

        mockMvc.perform(post("/events")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 20,
                                  "name": "Soutenance PFE - Groupe A",
                                  "description": "Présentation du projet de fin d'études"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Soutenance PFE - Groupe A"))
                .andExpect(jsonPath("$.categoryType").value("DEFENSE"))
                .andExpect(jsonPath("$.categoryName").value("PFE"))
                .andExpect(jsonPath("$.createdBy").value("Richard WOODWARD"));
    }


    @Test
    void shouldListAuditEvents_returns200() throws Exception {

        EventResponseDto dto = EventResponseDto.builder()
                .id(1L)
                .name("Audit Sprint 1")
                .description("Revue sprint 1")
                .categoryType(CategoryType.AUDIT)
                .categoryName("CI/CD")
                .createdBy("Sophie ROUSSEAU")
                .createdAt(LocalDateTime.now())
                .build();

        when(eventService.listByType(CategoryType.AUDIT)).thenReturn(List.of(dto));

        mockMvc.perform(get("/events/type/AUDIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Audit Sprint 1"))
                .andExpect(jsonPath("$[0].categoryType").value("AUDIT"))
                .andExpect(jsonPath("$[0].categoryName").value("CI/CD"))
                .andExpect(jsonPath("$[0].createdBy").value("Sophie ROUSSEAU"));

        verify(eventService).listByType(CategoryType.AUDIT);
    }

    @Test
    void shouldListDefenseEvents_returns200() throws Exception {

        EventResponseDto dto = EventResponseDto.builder()
                .id(2L)
                .name("Soutenance PFE - Groupe A")
                .description("Présentation du projet de fin d'études")
                .categoryType(CategoryType.DEFENSE)
                .categoryName("PFE")
                .createdBy("Richard WOODWARD")
                .createdAt(LocalDateTime.now())
                .build();

        when(eventService.listByType(CategoryType.DEFENSE)).thenReturn(List.of(dto));

        mockMvc.perform(get("/events/type/DEFENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Soutenance PFE - Groupe A"))
                .andExpect(jsonPath("$[0].categoryType").value("DEFENSE"))
                .andExpect(jsonPath("$[0].categoryName").value("PFE"));

        verify(eventService).listByType(CategoryType.DEFENSE);
    }

    @Test
    void shouldReturnEmptyList_whenNoEvents() throws Exception {

        when(eventService.listByType(CategoryType.AUDIT)).thenReturn(List.of());

        mockMvc.perform(get("/events/type/AUDIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
