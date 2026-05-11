package fr.horizon.backend_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.horizon.backend_api.model.dto.CategoryRequestCreateDto;
import fr.horizon.backend_api.model.dto.CategoryRequestDto;
import fr.horizon.backend_api.service.CategoryRequestService;
import fr.horizon.backend_api.service.JwtToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryRequestService categoryRequestService;

    @MockitoBean
    private JwtToken jwtToken;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGetAllCategoryRequests() throws Exception {

        CategoryRequestDto dto = CategoryRequestDto.builder()
                .id(1L)
                .name("IT")
                .subject("Réseau")
                .status("PENDING")
                .requesterId(10L)
                .requesterName("Alice")
                .description("desc")
                .build();

        when(categoryRequestService.findAll())
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/category-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("IT"));
    }

    @Test
    void shouldCreateCategoryRequest() throws Exception {

        CategoryRequestCreateDto input = CategoryRequestCreateDto.builder()
                .name("IT")
                .subject("Réseau")
                .requesterId(1L)
                .description("desc")
                .build();

        CategoryRequestDto output = CategoryRequestDto.builder()
                .id(1L)
                .name("IT")
                .subject("Réseau")
                .status("PENDING")
                .requesterId(1L)
                .requesterName("Bob")
                .description("desc")
                .build();

        when(categoryRequestService.create(any()))
                .thenReturn(output);

        mockMvc.perform(post("/category-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("IT"))
                .andExpect(jsonPath("$.requesterName").value("Bob"));
    }

    @Test
    void shouldDeleteCategoryRequest() throws Exception {

        mockMvc.perform(delete("/category-requests/1"))
                .andExpect(status().isOk());

        verify(categoryRequestService, times(1)).deleteById(1L);
    }
}
