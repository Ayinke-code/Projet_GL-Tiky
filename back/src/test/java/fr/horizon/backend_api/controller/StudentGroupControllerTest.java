package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.StudentGroupCreateDto;
import fr.horizon.backend_api.model.dto.StudentGroupDto;
import fr.horizon.backend_api.model.enums.GroupType;
import fr.horizon.backend_api.service.JwtToken;
import fr.horizon.backend_api.service.StudentGroupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentGroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentGroupService studentGroupService;

    @MockitoBean
    private JwtToken jwtToken;

    @MockitoBean
    private Authentication authentication;

    @Test
    void shouldCreateGroup_returns201() throws Exception {

        StudentGroupDto response = StudentGroupDto.builder()
                .id(1L)
                .name("Groupe LD")
                .type(GroupType.OPTION)
                .categoryId(10L)
                .categoryName("Logiciel & DevOps")
                .build();

        when(studentGroupService.create(any(StudentGroupCreateDto.class), anyString())).thenReturn(response);
        when(authentication.getName()).thenReturn("teacher@teacher.fr");

        mockMvc.perform(post("/groups")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Groupe LD",
                                  "type": "OPTION",
                                  "categoryId": 10,
                                  "memberIds": [1, 2, 3]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Groupe LD"))
                .andExpect(jsonPath("$.type").value("OPTION"))
                .andExpect(jsonPath("$.categoryName").value("Logiciel & DevOps"));

        verify(studentGroupService).create(any(StudentGroupCreateDto.class), anyString());
    }

    @Test
    void shouldCreateProjectGroup_withParentGroup_returns201() throws Exception {

        StudentGroupDto response = StudentGroupDto.builder()
                .id(2L)
                .name("Projet Horizon A")
                .type(GroupType.PROJECT)
                .categoryId(10L)
                .categoryName("Projet Horizon")
                .parentGroupId(100L)
                .parentGroupName("LD — Promo 2026")
                .build();

        when(studentGroupService.create(any(StudentGroupCreateDto.class), anyString())).thenReturn(response);
        when(authentication.getName()).thenReturn("teacher@teacher.fr");

        mockMvc.perform(post("/groups")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Projet Horizon A",
                                  "type": "PROJECT",
                                  "categoryId": 10,
                                  "parentGroupId": 100,
                                  "memberIds": [1, 2]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Projet Horizon A"))
                .andExpect(jsonPath("$.type").value("PROJECT"))
                .andExpect(jsonPath("$.parentGroupId").value(100))
                .andExpect(jsonPath("$.parentGroupName").value("LD — Promo 2026"));
    }

    @Test
    void shouldListAllGroups_returns200() throws Exception {

        StudentGroupDto g1 = StudentGroupDto.builder()
                .id(1L)
                .name("Groupe LD")
                .type(GroupType.OPTION)
                .categoryId(10L)
                .categoryName("Logiciel & DevOps")
                .build();

        StudentGroupDto g2 = StudentGroupDto.builder()
                .id(2L)
                .name("Projet Alpha")
                .type(GroupType.PROJECT)
                .categoryId(10L)
                .categoryName("Projet Horizon")
                .build();

        when(studentGroupService.findAll()).thenReturn(List.of(g1, g2));

        mockMvc.perform(get("/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Groupe LD"))
                .andExpect(jsonPath("$[1].name").value("Projet Alpha"));
    }

    @Test
    void shouldListAllGroups_returns200_withEmptyList() throws Exception {
        when(studentGroupService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldListGroupsByType_OPTION_returns200() throws Exception {

        StudentGroupDto optionGroup = StudentGroupDto.builder()
                .id(1L)
                .name("Groupe LD")
                .type(GroupType.OPTION)
                .categoryId(10L)
                .categoryName("Logiciel & DevOps")
                .build();

        when(studentGroupService.findByType(GroupType.OPTION)).thenReturn(List.of(optionGroup));

        mockMvc.perform(get("/groups/type/OPTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("OPTION"))
                .andExpect(jsonPath("$[0].name").value("Groupe LD"));
    }

    @Test
    void shouldListGroupsByType_PROJECT_returns200() throws Exception {

        StudentGroupDto projectGroup = StudentGroupDto.builder()
                .id(2L)
                .name("Projet Alpha")
                .type(GroupType.PROJECT)
                .categoryId(10L)
                .categoryName("Projet Horizon")
                .parentGroupId(100L)
                .parentGroupName("LD — Promo 2026")
                .build();

        when(studentGroupService.findByType(GroupType.PROJECT)).thenReturn(List.of(projectGroup));

        mockMvc.perform(get("/groups/type/PROJECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("PROJECT"))
                .andExpect(jsonPath("$[0].parentGroupName").value("LD — Promo 2026"));
    }

    @Test
    void shouldListGroupsByType_returns200_withEmptyList() throws Exception {
        when(studentGroupService.findByType(GroupType.PROJECT)).thenReturn(List.of());

        mockMvc.perform(get("/groups/type/PROJECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldDeleteGroup_returns200() throws Exception {

        mockMvc.perform(delete("/groups/1"))
                .andExpect(status().isOk());

        verify(studentGroupService).deleteById(1L);
    }

    @Test
    void shouldFindGroupById_returns200() throws Exception {

        StudentGroupDto dto = StudentGroupDto.builder()
                .id(1L)
                .name("Groupe LD")
                .type(GroupType.OPTION)
                .categoryId(10L)
                .categoryName("Logiciel & DevOps")
                .build();

        when(studentGroupService.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Groupe LD"))
                .andExpect(jsonPath("$.type").value("OPTION"));

        verify(studentGroupService).findById(1L);
    }

    @Test
    void shouldFindMyGroups_returns200() throws Exception {

        StudentGroupDto dto = StudentGroupDto.builder()
                .id(1L)
                .name("Groupe LD")
                .type(GroupType.OPTION)
                .categoryId(10L)
                .categoryName("Logiciel & DevOps")
                .build();

        when(studentGroupService.findMyGroups(anyString(), any(GroupType.class))).thenReturn(List.of(dto));
        when(authentication.getName()).thenReturn("student@student.fr");

        mockMvc.perform(get("/groups/my-groups?type=OPTION").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Groupe LD"))
                .andExpect(jsonPath("$[0].type").value("OPTION"));

        verify(studentGroupService).findMyGroups("student@student.fr", GroupType.OPTION);
    }

    @Test
    void shouldUpdateGroup_returns200() throws Exception {

        StudentGroupDto dto = StudentGroupDto.builder()
                .id(1L)
                .name("Groupe LD Mis à jour")
                .type(GroupType.OPTION)
                .categoryId(10L)
                .categoryName("Logiciel & DevOps")
                .build();

        when(studentGroupService.update(any(Long.class), any(StudentGroupCreateDto.class), anyString())).thenReturn(dto);
        when(authentication.getName()).thenReturn("teacher@teacher.fr");

        mockMvc.perform(put("/groups/1")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Groupe LD Mis à jour",
                                  "type": "OPTION",
                                  "categoryId": 10,
                                  "memberIds": [1, 2, 3]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Groupe LD Mis à jour"));

        verify(studentGroupService).update(any(Long.class), any(StudentGroupCreateDto.class), anyString());
    }

    @Test
    void shouldAssignTeacher_returns200() throws Exception {

        StudentGroupDto dto = StudentGroupDto.builder()
                .id(1L)
                .name("Groupe LD")
                .type(GroupType.OPTION)
                .build();

        when(studentGroupService.assignTeacher(1L, 5L)).thenReturn(dto);

        mockMvc.perform(put("/groups/1/teachers/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(studentGroupService).assignTeacher(1L, 5L);
    }

    @Test
    void shouldUnassignTeacher_returns200() throws Exception {

        StudentGroupDto dto = StudentGroupDto.builder()
                .id(1L)
                .name("Groupe LD")
                .type(GroupType.OPTION)
                .build();

        when(studentGroupService.unassignTeacher(1L, 5L)).thenReturn(dto);

        mockMvc.perform(delete("/groups/1/teachers/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(studentGroupService).unassignTeacher(1L, 5L);
    }
}
