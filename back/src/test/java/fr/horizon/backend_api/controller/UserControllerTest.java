package fr.horizon.backend_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.repository.UserRepository;
import fr.horizon.backend_api.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /users devrait retourner tous les utilisateurs")
    void getAllUsers_Success() throws Exception {
        User user = User.builder().id(1L).name("User1").build();
        when(userRepository.findAll()).thenReturn(List.of(user));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("User1"));
    }

    @Test
    @DisplayName("POST /users/add devrait créer un utilisateur")
    void createUser_Success() throws Exception {
        UserDto dto = new UserDto();
        dto.setName("NewUser");
        dto.setEmail("new@test.com");
        dto.setPassword("securePassword123");
        dto.setRoleId(1L);

        User user = User.builder().id(1L).name("NewUser").build();
        when(userService.createUser(any())).thenReturn(user);

        mockMvc.perform(post("/users/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewUser"));
    }

    @Test
    @DisplayName("DELETE /users/{id} devrait supprimer l'utilisateur")
    void deleteUser_Success() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isOk());
        verify(userRepository).deleteById(1L);
    }
}
