package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.IssueCreateDto;
import fr.horizon.backend_api.model.dto.IssueDto;
import fr.horizon.backend_api.model.dto.IncompleteRequestDto;
import fr.horizon.backend_api.model.enums.IssueStatus;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.Conversation;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.IssueFollower;
import fr.horizon.backend_api.service.IssueService;
import fr.horizon.backend_api.service.IssueFollowerService;
import fr.horizon.backend_api.service.JwtToken;
import fr.horizon.backend_api.service.ConversationService;
import fr.horizon.backend_api.websocket.ConversationWebSocketHandler;

import java.util.Optional;
import fr.horizon.backend_api.model.dto.RequestManagerDto;
import fr.horizon.backend_api.model.enums.RequestManagerStatus;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @MockitoBean
    private IssueFollowerService issueFollowerService;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private ConversationWebSocketHandler conversationWebSocketHandler;

    @Autowired
    private IssueController issueController;

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
    void shouldCreateIssue() {

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

        when(issueService.createIssue(any(), any(), anyString()))
                .thenReturn(response);

        when(authentication.getName()).thenReturn("john@test.com");

        IssueCreateDto dto = new IssueCreateDto();
        dto.setTitle("Bug");
        dto.setDescription("Something is broken");
        dto.setCategoryId(1L);

        // Appel direct — bypass total de MockMvc et du binding multipart
        IssueDto result = issueController.createIssue(dto, List.of(), authentication);

        assertThat(result.getTitle()).isEqualTo("Bug");
        assertThat(result.getCategoryName()).isEqualTo("DEV");

        verify(issueService).createIssue(
                any(IssueCreateDto.class),
                any(),
                eq("john@test.com")
        );
    }

    @Test
    void shouldDeleteIssue() throws Exception {

        when(authentication.getName()).thenReturn("john@test.com");

        mockMvc.perform(delete("/issues/1").principal(authentication))
                .andExpect(status().isOk());

        verify(issueService).deleteById(1L, "john@test.com");
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
        Long issueId = 1L;
        String email = "john@test.com";
        User user = User.builder().id(1L).email(email).build();
        Conversation conversation = Conversation.builder().id(10L).build();

        when(authentication.getName()).thenReturn(email);
        when(issueFollowerService.getUserByEmail(email)).thenReturn(user);
        when(conversationService.getConversationByIssueId(issueId)).thenReturn(Optional.of(conversation));
        when(issueFollowerService.getFollowedIssues(user)).thenReturn(List.of());

        mockMvc.perform(post("/issues/1/update-private").principal(authentication))
                .andExpect(status().isOk());

        verify(issueService).updatePrivacy(issueId, email);
        verify(conversationWebSocketHandler).broadcastToConversation(eq(10L), contains("privacy_update"));
        verify(issueFollowerService).getFollowedIssues(user);
    }

    @Test
    void shouldFollowIssue() throws Exception {
        Issue issue = Issue.builder().id(1L).build();
        User user = User.builder().id(2L).name("Test User").build();
        IssueFollower follower = IssueFollower.builder()
                .issue(issue)
                .user(user)
                .build();

        when(issueFollowerService.getUserByEmail(anyString())).thenReturn(user);
        when(issueFollowerService.followIssue(anyLong(), anyString(), any(User.class)))
                .thenReturn(follower);
        when(authentication.getName()).thenReturn("user@test.com");

        mockMvc.perform(post("/issues/1/follow")
                        .param("reason", "INTERESTED")
                        .principal(authentication))
                .andExpect(status().isCreated());

        verify(issueFollowerService).followIssue(eq(1L), eq("INTERESTED"), any(User.class));
    }

    @Test
    void shouldReturn409WhenAlreadyFollowing() throws Exception {
        when(issueFollowerService.getUserByEmail(anyString())).thenReturn(new User());
        when(issueFollowerService.followIssue(anyLong(), anyString(), any(User.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Vous suivez déjà cette issue"));
        when(authentication.getName()).thenReturn("user@test.com");

        mockMvc.perform(post("/issues/1/follow")
                        .param("reason", "INTERESTED")
                        .principal(authentication))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldGetFollowedIssues() throws Exception {
        when(issueFollowerService.getUserByEmail(anyString()))
                .thenReturn(new User());
        when(issueFollowerService.getFollowedIssues(any(User.class)))
                .thenReturn(List.of());
        when(authentication.getName()).thenReturn("user@test.com");

        mockMvc.perform(get("/issues/followed")
                        .principal(authentication))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetFollowStatus() throws Exception {
        when(issueFollowerService.getUserByEmail(anyString()))
                .thenReturn(new User());
        when(issueFollowerService.isFollowing(anyLong(), any(User.class)))
                .thenReturn(false);
        when(authentication.getName()).thenReturn("user@test.com");

        mockMvc.perform(get("/issues/1/follow/status")
                        .principal(authentication))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAssignManager() throws Exception {
        RequestManagerDto dto = RequestManagerDto.builder().id(10L).status(RequestManagerStatus.PENDING.name()).build();
        when(issueService.assignManagerToIssue(1L, 2L)).thenReturn(dto);

        mockMvc.perform(post("/issues/assign-manager/issue/1/target-user/2"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void shouldRespondToRequest() throws Exception {
        RequestManagerDto dto = RequestManagerDto.builder().id(10L).status(RequestManagerStatus.ACCEPTED.name()).build();
        when(issueService.respondManagerRequest(true, 10L, 2L)).thenReturn(dto);

        mockMvc.perform(put("/issues/demandes-manager/10/respond")
                        .param("accept", "true")
                        .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void shouldUnfollowIssue() throws Exception {
        User user = new User();
        when(authentication.getName()).thenReturn("user@test.com");
        when(issueFollowerService.getUserByEmail("user@test.com")).thenReturn(user);

        mockMvc.perform(delete("/issues/1/follow").principal(authentication))
                .andExpect(status().isNoContent());

        verify(issueFollowerService).unfollowIssue(eq(1L), any(User.class));
    }

    @Test
    void shouldGetTeacherAssignations() throws Exception {
        when(issueService.getTeacherAssignations(1L)).thenReturn(List.of());

        mockMvc.perform(get("/issues/demandes-manager/teacher").param("userId", "1"))
                .andExpect(status().isOk());

        verify(issueService).getTeacherAssignations(1L);
    }

    @Test
    void shouldGetOwnerAlerts() throws Exception {
        when(issueService.alertCategoryOwner(1L)).thenReturn(List.of());

        mockMvc.perform(get("/issues/demandes-manager/owner-alerts").param("userId", "1"))
                .andExpect(status().isOk());

        verify(issueService).alertCategoryOwner(1L);
    }

    @Test
    void shouldGetHistoricalRequests() throws Exception {
        when(issueService.getHistoricalRequestManager(1L)).thenReturn(List.of());

        mockMvc.perform(get("/issues/demandes-manager/history").param("userId", "1"))
                .andExpect(status().isOk());

        verify(issueService).getHistoricalRequestManager(1L);
    }

    @Test
    void shouldGetIncompleteIssues() throws Exception {
        IssueDto dto = IssueDto.builder().id(1L).title("Incomplet").build();
        when(issueService.getIncompleteIssuesToResent(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/issues/incomplete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void shouldReturn204WhenNoIncompleteIssues() throws Exception {
        when(issueService.getIncompleteIssuesToResent(1L)).thenReturn(List.of());

        mockMvc.perform(get("/issues/incomplete/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldMarkIssueAsIncomplete() throws Exception {
        mockMvc.perform(put("/issues/1/mark-incomplete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commentaire\":\"manque info\",\"raison\":\"raison\"}"))
                .andExpect(status().isOk());

        verify(issueService).markIssueAsIncomplete(eq(1L), any(), any());
    }

    @Test
    void shouldReturn404WhenMarkIncompleteNotFound() throws Exception {
        doThrow(new jakarta.persistence.EntityNotFoundException("not found"))
                .when(issueService).markIssueAsIncomplete(eq(99L), any(), any());

        mockMvc.perform(put("/issues/99/mark-incomplete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commentaire\":\"c\",\"raison\":\"r\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetIssueDTOById() throws Exception {
        IssueDto dto = IssueDto.builder().id(1L).title("Bug").build();
        when(authentication.getName()).thenReturn("john@test.com");
        when(issueService.getIssueDTOById(1L, "john@test.com")).thenReturn(dto);

        mockMvc.perform(get("/issues/dto/1").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldUpdateIssueWhenIncomplete() throws Exception {
        IssueDto dto = IssueDto.builder().id(1L).title("Corrigé").build();
        when(authentication.getName()).thenReturn("john@test.com");
        when(issueService.updateIssueWhenIncomplete(eq("john@test.com"), eq(1L), any())).thenReturn(dto);

        mockMvc.perform(post("/issues/update/incomplete/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Corrigé\",\"description\":\"desc\",\"categoryId\":1}")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Corrigé"));
    }

    @Test
    void shouldCreateRequestReopening() throws Exception {
        mockMvc.perform(post("/issues/1/request-reopening")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"Besoin de réouverture\""))
                .andExpect(status().isOk());

        verify(issueService).requestReopening(any(), eq(1L));
    }

    @Test
    void shouldUpdateStatusWithWebSocketNotification() throws Exception {
        Conversation conv = Conversation.builder().id(5L).build();
        when(conversationService.getConversationByIssueId(1L)).thenReturn(Optional.of(conv));

        mockMvc.perform(post("/issues/1/update-status")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("OUVERT"))
                .andExpect(status().isOk());

        verify(issueService).updateStatus("OUVERT", 1L);
        verify(conversationWebSocketHandler).broadcastToConversation(eq(5L), contains("status_update"));
    }
}
