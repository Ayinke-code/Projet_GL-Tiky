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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtToken jwtToken;

    @MockitoBean
    private Authentication authentication;

    @Test
    void shouldGetAllCategories() throws Exception {

        CategoryDto dto = CategoryDto.builder()
                .id(1L)
                .name("DEV")
                .subject("Backend Java")
                .build();

        when(categoryService.findAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("DEV"))
                .andExpect(jsonPath("$[0].subject").value("Backend Java"));

        verify(categoryService).findAll();
    }

    @Test
    void shouldCreateCategory() throws Exception {

        CategoryDto response = CategoryDto.builder()
                .id(1L)
                .name("DEV")
                .subject("Backend Java")
                .build();

        when(categoryService.createCategory(any())).thenReturn(response);

        mockMvc.perform(post("/categories")
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

        mockMvc.perform(delete("/categories/1"))
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

        when(authentication.getName()).thenReturn("user@test.com");
        when(categoryService.findById(1L)).thenReturn(dto);

        mockMvc.perform(post("/categories/followers/1")
                        .principal(authentication))
                .andExpect(status().isOk());

        verify(categoryService).addFollower(1L, "user@test.com");
        verify(categoryService).findById(1L);
    }

    @Test
    void shouldDeleteFollower() throws Exception {

        when(authentication.getName()).thenReturn("user@test.com");

        mockMvc.perform(delete("/categories/followers/1")
                        .principal(authentication))
                .andExpect(status().isOk());

        verify(categoryService).deleteFollowerById(1L, "user@test.com");
    }



    @Test
    void shouldGetCategoriesByType_audit() throws Exception {

        CategoryDto audit = CategoryDto.builder()
                .id(11L)
                .name("Test")
                .subject("Audit Test")
                .categoryType(CategoryType.AUDIT)
                .build();

        when(categoryService.findByCategoryType(CategoryType.AUDIT))
                .thenReturn(List.of(audit));

        mockMvc.perform(get("/categories/type/AUDIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test"))
                .andExpect(jsonPath("$[0].subject").value("Audit Test"))
                .andExpect(jsonPath("$[0].categoryType").value("AUDIT"));

        verify(categoryService).findByCategoryType(CategoryType.AUDIT);
    }

    @Test
    void shouldGetCategoriesByType_defense() throws Exception {

        CategoryDto defense = CategoryDto.builder()
                .id(20L)
                .name("PFE")
                .subject("Projet Fin Études")
                .categoryType(CategoryType.DEFENSE)
                .build();

        when(categoryService.findByCategoryType(CategoryType.DEFENSE))
                .thenReturn(List.of(defense));

        mockMvc.perform(get("/categories/type/DEFENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("PFE"))
                .andExpect(jsonPath("$[0].categoryType").value("DEFENSE"));

        verify(categoryService).findByCategoryType(CategoryType.DEFENSE);
    }

    @Test
    void shouldGetCategoriesByType_returnsEmpty() throws Exception {

        when(categoryService.findByCategoryType(CategoryType.AUDIT))
                .thenReturn(List.of());

        mockMvc.perform(get("/categories/type/AUDIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldGetFollowedCategories() throws Exception {
        CategoryDto dto = CategoryDto.builder()
                .id(1L)
                .name("DEV")
                .subject("Backend Java")
                .build();

        when(authentication.getName()).thenReturn("user@test.com");
        when(categoryService.findFollowedCategories("user@test.com")).thenReturn(List.of(dto));

        mockMvc.perform(get("/categories/followed")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("DEV"))
                .andExpect(jsonPath("$[0].subject").value("Backend Java"));

        verify(categoryService).findFollowedCategories("user@test.com");
    }
}

