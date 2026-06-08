package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.model.dto.UserFollowerDto;
import fr.horizon.backend_api.model.dto.UserRefDto;
import fr.horizon.backend_api.security.JwtAuthenticationFilter;
import fr.horizon.backend_api.service.JwtToken;
import fr.horizon.backend_api.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtToken jwtToken;

    @Test
    void shouldGetAllUsers() throws Exception {

        UserDto dto = UserDto.builder()
                .id(1L)
                .name("John")
                .email("john@test.com")
                .build();

        when(userService.findAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("John"))
                .andExpect(jsonPath("$[0].email").value("john@test.com"));

        verify(userService).findAll();
    }

    @Test
    void shouldCreateUser() throws Exception {

        UserDto response = UserDto.builder()
                .id(1L)
                .name("John")
                .email("john@test.com")
                .build();

        when(userService.createUser(any())).thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "John",
                              "email": "john@test.com",
                              "password": "password123",
                              "roleIds": [1]
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(userService).createUser(any());
    }

    @Test
    void shouldDeleteUser() throws Exception {

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isOk());

        verify(userService).deleteById(1L);
    }

    // ✅ AJOUT : test endpoint byRoles
    @Test
    void shouldGetUsersByRoles() throws Exception {

        UserRefDto dto = UserRefDto.builder()
                .id(1L)
                .name("Teacher")
                .roles(Set.of("TEACHER"))
                .build();

        when(userService.findAllByRoles(List.of("TEACHER")))
                .thenReturn(List.of(dto));

        mockMvc.perform(post("/users/byRoles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            ["TEACHER"]
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Teacher"))
                .andExpect(jsonPath("$[0].roles[0]").value("TEACHER"));

        verify(userService).findAllByRoles(List.of("TEACHER"));
    }

    @Test
    void shouldGetAllTeachersWithCategoryDetail() throws Exception {

        UserFollowerDto dto = UserFollowerDto.builder()
                .id(1L)
                .name("Prof Martin")
                .follower(true)
                .build();

        when(userService.getAllTeachersWithCategoryDetail(5L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/users/teachers/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Prof Martin"))
                .andExpect(jsonPath("$[0].follower").value(true));

        verify(userService).getAllTeachersWithCategoryDetail(5L);
    }
}