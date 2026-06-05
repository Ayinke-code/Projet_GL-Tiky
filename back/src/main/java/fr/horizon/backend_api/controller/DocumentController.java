package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.service.DocumentService;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        return documentService.download(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}