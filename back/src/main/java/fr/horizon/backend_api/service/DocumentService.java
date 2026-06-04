package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Document;
import fr.horizon.backend_api.repository.DocumentRepository;
import org.springframework.core.io.Resource;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    /**
     * Télécharge un document stocké sur le serveur.
     * <p>
     * Récupère le document en base de données, charge le fichier depuis le disque,
     * et retourne une réponse HTTP contenant le fichier en mode téléchargement.
     *
     * @param id identifiant du document à télécharger
     * @return réponse HTTP contenant le fichier en Resource
     * @throws IOException si une erreur survient lors de la lecture du fichier
     */
    public ResponseEntity<Resource> download(Long id) throws IOException {

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found"));

        Path filePath = Paths.get(doc.getPath());

        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("File not readable");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getOriginalName() + "\"")
                .body(resource);
    }

    /**
     * Supprime un document à la fois en base de données et sur le disque.
     * <p>
     * Cette opération supprime :
     * - le fichier physique stocké sur le serveur
     * - l'enregistrement correspondant dans la table Document
     *
     * @param id identifiant du document à supprimer
     * @throws RuntimeException si la suppression du fichier échoue
     * @throws EntityNotFoundException si le document n'existe pas
     */
    @Transactional
    public void delete(Long id) {

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found"));

        // Suppression fichier disque
        Path filePath = Paths.get(doc.getPath());

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Error deleting file from disk", e);
        }

        // Suppression dans la table Document
        documentRepository.delete(doc);
    }
}
