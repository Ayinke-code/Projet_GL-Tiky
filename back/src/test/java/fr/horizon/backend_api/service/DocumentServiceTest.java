package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Document;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.Message;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.repository.DocumentRepository;
import fr.horizon.backend_api.repository.MessageRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentService documentService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @Test
    void shouldDownloadDocumentSuccessfully() throws IOException {

        // Crée un vrai fichier temporaire sur le disque
        Path file = tempDir.resolve("test.pdf");
        Files.write(file, "pdf content".getBytes());

        Document doc = new Document();
        doc.setId(1L);
        doc.setPath(file.toString());
        doc.setOriginalName("test.pdf");
        doc.setContentType("application/pdf");

        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        ResponseEntity<Resource> response = documentService.download(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType())
                .hasToString("application/pdf");
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("attachment; filename=\"test.pdf\"");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().exists()).isTrue();
    }

    @Test
    void shouldThrowWhenDocumentNotFoundOnDownload() {

        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.download(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Document not found");
    }

    @Test
    void shouldThrowWhenFileNotReadableOnDownload() {

        // Chemin qui n'existe pas sur le disque
        Document doc = new Document();
        doc.setId(2L);
        doc.setPath(tempDir.resolve("missing.pdf").toString());
        doc.setOriginalName("missing.pdf");
        doc.setContentType("application/pdf");

        when(documentRepository.findById(2L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.download(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("File not readable");
    }

    @Test
    void shouldDeleteDocumentAndFileSuccessfully() throws IOException {

        Path file = tempDir.resolve("to-delete.pdf");
        Files.write(file, "data".getBytes());

        Document doc = new Document();
        doc.setId(3L);
        doc.setPath(file.toString());

        when(documentRepository.findById(3L)).thenReturn(Optional.of(doc));

        documentService.delete(3L);

        // Fichier supprimé du disque
        assertThat(Files.exists(file)).isFalse();

        // Supprimé en base
        verify(documentRepository).delete(doc);
    }

    @Test
    void shouldDeleteDocumentEvenIfFileDoesNotExist() {

        // Fichier déjà absent du disque — deleteIfExists ne doit pas planter
        Document doc = new Document();
        doc.setId(4L);
        doc.setPath(tempDir.resolve("already-gone.pdf").toString());

        when(documentRepository.findById(4L)).thenReturn(Optional.of(doc));

        assertThatCode(() -> documentService.delete(4L))
                .doesNotThrowAnyException();

        verify(documentRepository).delete(doc);
    }

    @Test
    void shouldThrowWhenDocumentNotFoundOnDelete() {

        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Document not found");

        verify(documentRepository, never()).delete(any());
    }

    @Test
    void shouldStoreFilesSuccessfully() throws IOException {

        Message message = new Message();
        Issue issue = new Issue();
        User user = new User();

        message.setId(1L);
        message.setIssue(issue);
        message.setUser(user);

        MultipartFile file = mock(MultipartFile.class);

        when(messageRepository.findById(1L))
                .thenReturn(Optional.of(message));

        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("file.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(100L);

        when(fileStorageService.store(file)).thenReturn("uuid.pdf");

        when(documentRepository.saveAll(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = documentService.uploadWithMessage(1L, List.of(file));

        assertThat(result).hasSize(1);

        verify(fileStorageService).store(file);
        verify(documentRepository).saveAll(any());
    }
}