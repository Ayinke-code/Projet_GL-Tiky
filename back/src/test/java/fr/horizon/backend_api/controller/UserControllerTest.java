package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.model.dto.UserRequestDto;
import fr.horizon.backend_api.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

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
                          "email": "john@test.com"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(userService).createUser(any());
    }

    @Test
    void shouldDeleteUser() throws Exception {

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isOk()); // ou isNoContent()

        verify(userService).deleteById(1L);
    }
}