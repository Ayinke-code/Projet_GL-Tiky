package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.CategoryDetailsDto;
import fr.horizon.backend_api.model.dto.CategoryDto;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.service.CategoryService;
import fr.horizon.backend_api.service.JwtToken;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(username = "test", roles = "TEACHER")
@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc
class CategoryControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private CategoryService categoryService;

        @MockitoBean
        private JwtToken jwtToken;

        @Test
        void shouldGetAllCategories() throws Exception {

                CategoryDto dto = CategoryDto.builder()
                                .id(1L)
                                .name("DEV")
                                .subject("Backend Java")
                                .build();

                when(categoryService.findAll(anyString())).thenReturn(List.of(dto));

                mockMvc.perform(get("/categories"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("DEV"))
                                .andExpect(jsonPath("$[0].subject").value("Backend Java"));

                verify(categoryService).findAll("test");
        }

        @Test
        void shouldCreateCategory() throws Exception {

                CategoryDto response = CategoryDto.builder()
                                .id(1L)
                                .name("DEV")
                                .subject("Backend Java")
                                .build();

                when(categoryService.createCategory(any())).thenReturn(response);

                mockMvc.perform(post("/categories").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                    {
                                                      "name": "DEV",
                                                      "subject": "Backend Java",
                                                      "ownerId": 1
                                                    }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.name").value("DEV"));

                verify(categoryService).createCategory(any());
        }

        @Test
        void shouldDeleteCategory() throws Exception {

                mockMvc.perform(delete("/categories/1").with(csrf()))
                                .andExpect(status().isOk());

                verify(categoryService).deleteById(1L);
        }

        @Test
        void shouldGetCategoryById() throws Exception {

                CategoryDetailsDto dto = CategoryDetailsDto.builder().build();

                when(categoryService.findById(1L)).thenReturn(dto);

                mockMvc.perform(get("/categories/1"))
                                .andExpect(status().isOk());

                verify(categoryService).findById(1L);
        }

        @Test
        void shouldAddFollower() throws Exception {

                CategoryDetailsDto dto = CategoryDetailsDto.builder().build();

                when(categoryService.findById(1L)).thenReturn(dto);

                mockMvc.perform(post("/categories/followers/10/1").with(csrf()))
                                .andExpect(status().isOk());

                verify(categoryService).addFollower(1L, 10L);
                verify(categoryService).findById(1L);
        }

        @Test
        void shouldDeleteFollower() throws Exception {

                mockMvc.perform(delete("/categories/followers/10/1").with(csrf()))
                                .andExpect(status().isOk());

                verify(categoryService).deleteFollowerById(1L, 10L);
        }

        @Test
        void shouldGetCategoriesByType_audit() throws Exception {

                CategoryDto audit = CategoryDto.builder()
                                .id(11L)
                                .name("Test")
                                .subject("Audit Test")
                                .categoryType(CategoryType.AUDIT)
                                .build();

                when(categoryService.findByCategoryType(eq(CategoryType.AUDIT), anyString())).thenReturn(List.of(audit));

                mockMvc.perform(get("/categories/type/AUDIT").with(user("test")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("Test"))
                                .andExpect(jsonPath("$[0].subject").value("Audit Test"))
                                .andExpect(jsonPath("$[0].categoryType").value("AUDIT"));

                verify(categoryService).findByCategoryType(CategoryType.AUDIT, "test");
        }

        @Test
        void shouldGetCategoriesByType_defense() throws Exception {

                CategoryDto defense = CategoryDto.builder()
                                .id(20L)
                                .name("PFE")
                                .subject("Projet Fin Études")
                                .categoryType(CategoryType.DEFENSE)
                                .build();

                when(categoryService.findByCategoryType(eq(CategoryType.DEFENSE), anyString())).thenReturn(List.of(defense));

                mockMvc.perform(get("/categories/type/DEFENSE").with(user("test")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("PFE"))
                                .andExpect(jsonPath("$[0].categoryType").value("DEFENSE"));

                verify(categoryService).findByCategoryType(CategoryType.DEFENSE, "test");
        }

        @Test
        void shouldGetCategoriesByType_returnsEmpty() throws Exception {

                when(categoryService.findByCategoryType(eq(CategoryType.AUDIT), anyString())).thenReturn(List.of());

                mockMvc.perform(get("/categories/type/AUDIT").with(user("test")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$").isEmpty());
        }
}
