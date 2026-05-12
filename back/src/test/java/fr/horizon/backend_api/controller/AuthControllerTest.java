package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.LoginResponse;
import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.service.AuthService;
import fr.horizon.backend_api.service.JwtToken;
import fr.horizon.backend_api.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.Authentication;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.TestPropertySource(properties = {
        "jwt.expiration=3600000"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private Authentication authentication;

    @MockitoBean
    private JwtToken jwtToken;

    @Test
    void shouldLogin_and_set_cookie() throws Exception {

        LoginResponse response = LoginResponse.builder()
                .id(1L)
                .name("John")
                .token("jwt-token")
                .roles(Set.of("USER"))
                .email("test@test.com")
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "email": "test@test.com",
                          "password": "password"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("token"));

        verify(authService).login(any());
    }

    @Test
    void shouldReturn_current_user() throws Exception {

        when(authentication.getName()).thenReturn("test@test.com");

        User user = new User();
        user.setEmail("test@test.com");

        UserDto dto = UserDto.builder()
                .id(1L)
                .email("test@test.com")
                .name("John")
                .build();

        when(userService.findByEmail("test@test.com")).thenReturn(user);
        when(userService.toDto(user)).thenReturn(dto);

        mockMvc.perform(get("/auth/me")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andExpect(jsonPath("$.name").value("John"));

        verify(userService).findByEmail("test@test.com");
    }

    @Test
    void should_logout_and_clear_cookie() throws Exception {

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("token"))
                .andExpect(cookie().maxAge("token", 0));
    }
}
