package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.EventInvitationResponseDto;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

        mockMvc.perform(post("/events/school")
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

        mockMvc.perform(post("/events/school")
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

    @Test
    void shouldDeleteEvent_returns200() throws Exception {

        mockMvc.perform(delete("/events/1"))
                .andExpect(status().isOk());

        verify(eventService).deleteById(1L);
    }

    @Test
    void shouldCreateAuditEvent_whenUserIsOptionLeader_returns201() throws Exception {

        EventResponseDto response = EventResponseDto.builder()
                .id(10L)
                .name("Audit Sprint 3 — Option")
                .description("Revue du code du sprint 3 par l'option leader")
                .categoryType(CategoryType.AUDIT)
                .categoryName("UI/UX")
                .createdBy("Paul MARTIN")
                .createdAt(LocalDateTime.now())
                .build();

        when(eventService.create(any(), anyString())).thenReturn(response);
        when(authentication.getName()).thenReturn("teacher2@teacher.fr");

        mockMvc.perform(post("/events/school")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 10,
                                  "name": "Audit Sprint 3 — Option",
                                  "description": "Revue du code du sprint 3 par l'option leader"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Audit Sprint 3 — Option"))
                .andExpect(jsonPath("$.categoryType").value("AUDIT"))
                .andExpect(jsonPath("$.categoryName").value("UI/UX"))
                .andExpect(jsonPath("$.createdBy").value("Paul MARTIN"));

        verify(eventService).create(any(), anyString());
    }

    @Test
    void shouldInviteUserToEvent_returns200() throws Exception {
        EventResponseDto response = EventResponseDto.builder()
                .id(1L)
                .name("Réunion technique")
                .build();

        when(eventService.inviteUserToMeeting(1L, 42L)).thenReturn(response);

        mockMvc.perform(post("/events/1/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 42
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Réunion technique"));

        verify(eventService).inviteUserToMeeting(1L, 42L);
    }

    @Test
    void shouldInviteUsersToEvent_bulk_returns200() throws Exception {
        EventResponseDto response = EventResponseDto.builder()
                .id(1L)
                .name("Réunion d'équipe")
                .build();

        when(eventService.inviteUsersToMeeting(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/events/1/invitations/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[42, 43, 44]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Réunion d'équipe"));

        verify(eventService).inviteUsersToMeeting(eq(1L), any());
    }

    @Test
    void shouldRespondToInvitation_returns200() throws Exception {
        when(authentication.getName()).thenReturn("student@student.fr");

        EventInvitationResponseDto responseDto = new EventInvitationResponseDto();

        when(eventService.responseToMeetingInvitation(eq(1L), anyBoolean(), eq("student@student.fr"), anyString()))
                .thenReturn(responseDto);

        mockMvc.perform(put("/events/1/invitations/respond")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "response": true,
                              "raison": "Je serai présent"
                            }
                            """))
                .andExpect(status().isOk());

        verify(eventService).responseToMeetingInvitation(1L, true, "student@student.fr", "Je serai présent");
    }

    @Test
    void shouldCreateMeeting_forIssue_returns201() throws Exception {
        EventResponseDto response = EventResponseDto.builder()
                .id(5L)
                .name("Point d'avancement Ticket #100")
                .createdBy("Scrum Master")
                .build();

        when(authentication.getName()).thenReturn("scrum@master.fr");
        when(eventService.createMeeting(any(), eq("scrum@master.fr"), eq(100L))).thenReturn(response);

        mockMvc.perform(post("/events/meeting/100")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Point d'avancement",
                                  "description": "Discussion sur le bug"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.name").value("Point d'avancement Ticket #100"));

        verify(eventService).createMeeting(any(), eq("scrum@master.fr"), eq(100L));
    }

    @Test
    void shouldCheckUpcomingMeeting_returns200() throws Exception {
        when(eventService.issueGotMeeting(100L)).thenReturn(true);

        mockMvc.perform(get("/events/meeting/100/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(eventService).issueGotMeeting(100L);
    }

    @Test
    void shouldGetMyCreatedEvents_returns200() throws Exception {
        when(authentication.getName()).thenReturn("teacher@teacher.fr");

        EventResponseDto event = EventResponseDto.builder()
                .id(1L)
                .name("Mon événement")
                .build();

        when(eventService.getMyCreatedEvents("teacher@teacher.fr")).thenReturn(List.of(event));

        mockMvc.perform(get("/events/me/created")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mon événement"));

        verify(eventService).getMyCreatedEvents("teacher@teacher.fr");
    }

    @Test
    void shouldCancelEvent_returns204() throws Exception {
        mockMvc.perform(delete("/events/1/cancel"))
                .andExpect(status().isNoContent());

        verify(eventService).cancelEvent(1L);
    }

    @Test
    void shouldDeleteParticipantFromMeeting_returns200() throws Exception {
        EventResponseDto response = EventResponseDto.builder()
                .id(1L)
                .name("Réunion mise à jour")
                .build();

        when(eventService.deleteParticipantFromMeeting(1L, 42L)).thenReturn(response);

        mockMvc.perform(delete("/events/1/participants/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Réunion mise à jour"));

        verify(eventService).deleteParticipantFromMeeting(1L, 42L);
    }

    @Test
    void shouldRescheduleMeeting_returns200() throws Exception {
        EventResponseDto response = EventResponseDto.builder()
                .id(1L)
                .name("Réunion reportée")
                .build();

        when(eventService.updateMeetingDueToChange(eq(1L), any(), any(), eq(true))).thenReturn(response);

        mockMvc.perform(put("/events/1/reschedule")
                        .param("addAll", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-06-01T10:00:00",
                                  "endDate": "2026-06-01T11:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Réunion reportée"));

        verify(eventService).updateMeetingDueToChange(eq(1L), any(), any(), eq(true));
    }



    @Test
    void shouldGetMySessions_returns200() throws Exception {

        EventResponseDto session = EventResponseDto.builder()
                .id(1L)
                .name("Audit Sprint 3")
                .categoryType(CategoryType.AUDIT)
                .categoryName("UI/UX")
                .createdBy("Sophie ROUSSEAU")
                .createdAt(LocalDateTime.now())
                .build();

        when(eventService.listForStudent(anyString())).thenReturn(List.of(session));
        when(authentication.getName()).thenReturn("student@student.fr");

        mockMvc.perform(get("/events/my-sessions").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Audit Sprint 3"))
                .andExpect(jsonPath("$[0].categoryType").value("AUDIT"));

        verify(eventService).listForStudent("student@student.fr");
    }
}
