package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.exception.FileTooLargeException;
import fr.horizon.backend_api.exception.UnsupportedFileFormatException;
import fr.horizon.backend_api.service.DocumentService;
import fr.horizon.backend_api.service.JwtToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = DocumentController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class  // ← clé du problème
        }
)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private JwtToken jwtToken;

    @Test
    void shouldDownloadDocument() throws Exception {

        byte[] fileContent = "hello file".getBytes();
        Resource resource = new ByteArrayResource(fileContent);

        ResponseEntity<Resource> serviceResponse = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test.pdf\"")
                .body(resource);

        when(documentService.download(1L)).thenReturn(serviceResponse);

        mockMvc.perform(get("/documents/1/download"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"test.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(fileContent));

        verify(documentService).download(1L);
    }

    @Test
    void shouldDeleteDocument() throws Exception {

        doNothing().when(documentService).delete(1L);

        mockMvc.perform(delete("/documents/1"))
                .andExpect(status().isNoContent());

        verify(documentService).delete(1L);
    }

    /**
     * HTTP contract of a rejected format: 415 and the JSON error body the frontend displays.
     */
    @Test
    void shouldReturn415WhenFormatIsNotAllowed() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "files", "script.exe", "application/x-msdownload", "MZ".getBytes());

        when(documentService.uploadWithMessage(eq(1L), anyList()))
                .thenThrow(new UnsupportedFileFormatException("script.exe", "PDF, PNG, JPG, DOCX, XLSX"));

        mockMvc.perform(multipart("/documents/message/1").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error")
                        .value("Format non autorisé : script.exe (formats acceptés : PDF, PNG, JPG, DOCX, XLSX)"));
    }

    /**
     * HTTP contract of an oversized upload: 413 and the JSON error body the frontend displays.
     */
    @Test
    void shouldReturn413WhenFileIsTooLarge() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "files", "gros.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF".getBytes());

        when(documentService.uploadWithMessage(eq(1L), anyList()))
                .thenThrow(new FileTooLargeException("Fichier trop volumineux : gros.pdf (10 Mo max. par fichier)"));

        mockMvc.perform(multipart("/documents/message/1").file(file))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error")
                        .value("Fichier trop volumineux : gros.pdf (10 Mo max. par fichier)"));
    }
}