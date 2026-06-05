package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.DocumentDto;
import fr.horizon.backend_api.service.DocumentService;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Télécharge un document à partir de son identifiant.
     *
     * @param id identifiant du document
     * @return fichier en tant que Resource HTTP (stream)
     * @throws IOException en cas d'erreur de lecture du fichier sur disque
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        return documentService.download(id);
    }

    /**
     * Supprime un document.
     *
     * @param id identifiant du document à supprimer
     * @return réponse HTTP 204 (No Content) si suppression réussie
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Upload un ou plusieurs fichiers et les associe à un message.
     *
     * Utilisé lors de l'envoi d'un message avec pièces jointes.
     *
     * @param messageId identifiant du message cible
     * @param files fichiers envoyés depuis le frontend
     * @return liste des documents créés (DTO)
     */
    @PostMapping("/message/{messageId}")
    public ResponseEntity<List<DocumentDto>> uploadWithMessage(
            @PathVariable Long messageId,
            @RequestPart("files") List<MultipartFile> files
    ) {
        List<DocumentDto> created = documentService.uploadWithMessage(messageId, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}