package fr.horizon.backend_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.horizon.backend_api.model.dto.CreateCategoryDTO;
import fr.horizon.backend_api.model.dto.LoginRequest;
import fr.horizon.backend_api.model.dto.LoginResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.repository.UserRepository;
import fr.horizon.backend_api.repository.RoleRepository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Test d'intégration - Flux Création de Catégorie")
class CategoryFlowIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        String uniqueName;

        @BeforeEach
        void setup() {
                this.uniqueName = "Mathématiques " + System.currentTimeMillis();
                userRepository.deleteAll();
                Role teacherRole = roleRepository.findByName("TEACHER")
                                .orElseGet(() -> roleRepository.save(new Role(null, "TEACHER")));

                User testUser = User.builder()
                                .email("test@example.com")
                                .password(passwordEncoder.encode("password"))
                                .name("Test Teacher")
                                .role(teacherRole)
                                .build();
                userRepository.save(testUser);
        }

        @Test
        @DisplayName("Devrait permettre à un utilisateur de se connecter et de créer une catégorie")
        void shouldLoginAndCreateCategory() throws Exception {
                // Connexion (Login)
                LoginRequest loginRequest = new LoginRequest("test@example.com", "password");

                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andReturn();

                String responseContent = loginResult.getResponse().getContentAsString();
                LoginResponse loginResponse = objectMapper.readValue(responseContent, LoginResponse.class);
                String token = loginResponse.getToken();

                // Création de catégorie (Scenario étape 3, 4, 5)
                CreateCategoryDTO createDTO = CreateCategoryDTO.builder()
                                .name(this.uniqueName)
                                .subject("Algèbre Linéaire")
                                .build();

                mockMvc.perform(post("/api/categories")
                                .contextPath("/api")
                                .header("Authorization", "Bearer " + token)
                                .header("X-Teacher-ID", "teacher-default")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createDTO)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value(this.uniqueName))
                                .andExpect(jsonPath("$.subject").value("Algèbre Linéaire"));

                // Vérification (Scenario étape 6)
                mockMvc.perform(get("/api/categories/teacher")
                                .contextPath("/api")
                                .header("Authorization", "Bearer " + token)
                                .header("X-Teacher-ID", "teacher-default"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[?(@.name == '" + this.uniqueName + "')]").exists());
        }
}
