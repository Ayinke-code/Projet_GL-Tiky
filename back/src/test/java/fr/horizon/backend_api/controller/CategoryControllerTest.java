//package fr.horizon.backend_api.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import fr.horizon.backend_api.model.dto.CategoryResponseDto;
//import fr.horizon.backend_api.model.dto.CategoryCreateDto;
//import fr.horizon.backend_api.service.CategoryService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc(addFilters = false) // Disable security for easier API testing
//class CategoryControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockitoBean
//    private CategoryService categoryService;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Test
//    @DisplayName("GET /categories/active devrait retourner la liste des catégories actives")
//    void getAllActiveCategories_Success() throws Exception {
//        CategoryResponseDto cat = CategoryResponseDto.builder()
//                .id(1L)
//                .name("Maths")
//                .subject("Algèbre")
//                .status("active")
//                .build();
//
//        when(categoryService.getAllActiveCategories()).thenReturn(List.of(cat));
//
//        mockMvc.perform(get("/categories/active"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].name").value("Maths"))
//                .andExpect(jsonPath("$[0].subject").value("Algèbre"));
//    }
//
//    @Test
//    @DisplayName("POST /categories devrait créer une catégorie (Parcours complet Mock)")
//    void createCategory_FullFlow_Success() throws Exception {
//        CreateCategoryDto dto = CreateCategoryDto.builder()
//                .name("Physique")
//                .subject("Optique")
//                .build();
//
//        CategoryResponseDto response = CategoryResponseDto.builder()
//                .id(2L)
//                .name("Physique")
//                .subject("Optique")
//                .status("active")
//                .owner("teacher-1")
//                .build();
//
//        when(categoryService.createCategory(any(CreateCategoryDto.class), any(String.class))).thenReturn(response);
//
//        mockMvc.perform(post("/categories")
//                        .header("X-Teacher-ID", "teacher-1")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(dto)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.id").value(2))
//                .andExpect(jsonPath("$.name").value("Physique"))
//                .andExpect(jsonPath("$.subject").value("Optique"))
//                .andExpect(jsonPath("$.status").value("active"));
//    }
//
//    @Test
//    @DisplayName("GET /categories/{id} devrait retourner la catégorie si elle existe")
//    void getCategoryById_Success() throws Exception {
//        CategoryResponseDto response = CategoryResponseDto.builder()
//                .id(1L)
//                .name("Maths")
//                .build();
//
//        when(categoryService.getCategoryById(1L)).thenReturn(java.util.Optional.of(response));
//
//        mockMvc.perform(get("/categories/1"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.name").value("Maths"));
//    }
//
//    @Test
//    @DisplayName("GET /categories/{id} devrait retourner 404 si elle n'existe pas")
//    void getCategoryById_NotFound() throws Exception {
//        when(categoryService.getCategoryById(99L)).thenReturn(java.util.Optional.empty());
//
//        mockMvc.perform(get("/categories/99"))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    @DisplayName("PUT /categories/{id} devrait mettre à jour la catégorie")
//    void updateCategory_Success() throws Exception {
//        CreateCategoryDto dto = CreateCategoryDto.builder()
//                .name("Maths Updated")
//                .subject("Analyse")
//                .build();
//
//        CategoryResponseDto response = CategoryResponseDto.builder()
//                .id(1L)
//                .name("Maths Updated")
//                .build();
//
//        when(categoryService.updateCategory(eq(1L), any(), any())).thenReturn(response);
//
//        mockMvc.perform(put("/categories/1")
//                        .header("X-Teacher-ID", "teacher-1")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(dto)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.name").value("Maths Updated"));
//    }
//
//    @Test
//    @DisplayName("PATCH /categories/{id}/archive devrait archiver la catégorie")
//    void archiveCategory_Success() throws Exception {
//        CategoryResponseDto response = CategoryResponseDto.builder()
//                .id(1L)
//                .status("archived")
//                .build();
//
//        when(categoryService.archiveCategory(eq(1L), any())).thenReturn(response);
//
//        mockMvc.perform(patch("/categories/1/archive")
//                        .header("X-Teacher-ID", "teacher-1"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value("archived"));
//    }
//
//    @Test
//    @DisplayName("DELETE /categories/{id} devrait supprimer la catégorie")
//    void deleteCategory_Success() throws Exception {
//        mockMvc.perform(delete("/categories/1")
//                        .header("X-Teacher-ID", "teacher-1"))
//                .andExpect(status().isNoContent());
//    }
//
//    @Test
//    @DisplayName("POST /categories/{id}/follow devrait suivre la catégorie")
//    void followCategory_Success() throws Exception {
//        mockMvc.perform(post("/categories/1/follow")
//                        .header("X-Teacher-ID", "teacher-1"))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    @DisplayName("POST /categories avec nom en doublon devrait retourner 409")
//    void createCategory_Conflict() throws Exception {
//        CreateCategoryDto dto = CreateCategoryDto.builder()
//                .name("Duplicate")
//                .subject("Subject")
//                .build();
//
//        when(categoryService.createCategory(any(), any()))
//                .thenThrow(new fr.horizon.backend_api.exception.CategoryAlreadyExistsException("Already exists"));
//
//        mockMvc.perform(post("/categories")
//                        .header("X-Teacher-ID", "teacher-1")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(dto)))
//                .andExpect(status().isConflict())
//                .andExpect(jsonPath("$.error").value("CATEGORY_ALREADY_EXISTS"));
//    }
//}