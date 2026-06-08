package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.DocumentDeletionException;
import fr.horizon.backend_api.exception.FileDeletionException;
import fr.horizon.backend_api.exception.FileNotReadableException;
import fr.horizon.backend_api.exception.FileStorageException;
import fr.horizon.backend_api.model.Document;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.Message;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.DocumentDto;
import fr.horizon.backend_api.repository.DocumentRepository;
import fr.horizon.backend_api.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final MessageRepository messageRepository;
    private final FileStorageService fileStorageService;

    /**
     * Chemin vers le dossier contenant les fichiers attachés aux Tickets
     */
    @Value("${app.upload-dir}")
    private String uploadDir;

    /**
     * Télécharge un document stocké sur le serveur.
     */
    public ResponseEntity<Resource> download(Long id) throws IOException {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found"));

        Path filePath = Paths.get(doc.getPath());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new FileNotReadableException();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getOriginalName() + "\"")
                .body(resource);
    }

    /**
     * Supprime un document à la fois en base de données et sur le disque.
     */
    @Transactional
    public void delete(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found"));

        Path filePath = Paths.get(doc.getPath());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new FileDeletionException("Error deleting file from disk", e);
        }

        documentRepository.delete(doc);
    }

    /**
     * Supprime tous les documents selon une issue,
     * à la fois en base de données et sur le disque.
     */
    @Transactional
    public void deleteDocumentsByIssueId(Long issueId) {

        List<Document> documents = documentRepository.findByIssueId(issueId);

        for (Document document : documents) {
            try {
                Path filePath = Paths.get(document.getPath());

                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }

            } catch (IOException e) {
                throw new DocumentDeletionException(
                        "Erreur lors de la suppression du fichier : " + document.getStoredName(),
                        e
                );
            }
        }

        documentRepository.deleteByIssueId(issueId);
    }

    /**
     * Attache des fichiers à un message existant.
     * <p>
     * Récupère le message et son issue parente, puis délègue le stockage
     * à {@link #storeFiles}. Chaque document créé est lié à la fois
     * à l'issue et au message.
     *
     * @param messageId identifiant du message auquel attacher les fichiers
     * @param files     fichiers à attacher (peut être vide)
     * @return liste des documents créés sous forme de DTO
     * @throws EntityNotFoundException si le message n'existe pas
     */
    @Transactional
    public List<DocumentDto> uploadWithMessage(Long messageId, List<MultipartFile> files) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));

        List<Document> documents = storeFiles(files, message.getIssue(), message.getUser(), message);

        return documents.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Méthode partagée : utilisée par IssueService
     * Stocke et persiste une liste de fichiers liés à une issue.
     * <p>
     * Méthode générique utilisée à la fois lors de la création d'une issue
     * (sans message) et lors de l'envoi d'un message (avec message).
     * Les fichiers vides sont ignorés silencieusement.
     *
     * @param files      fichiers à traiter (peut être null ou vide)
     * @param issue      issue parente à laquelle rattacher les documents
     * @param uploadedBy utilisateur effectuant l'upload
     * @param message    message auquel lier les documents, ou {@code null} si aucun
     * @return liste des documents persistés
     * @throws FileStorageException si une erreur survient lors du stockage d'un fichier
     */
    public List<Document> storeFiles(List<MultipartFile> files, Issue issue, User uploadedBy, Message message) {
        if (files == null || files.isEmpty()) return List.of();

        List<Document> documents = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            try {
                String storedName = fileStorageService.store(file);

                Document document = Document.builder()
                        .originalName(file.getOriginalFilename())
                        .storedName(storedName)
                        .path(uploadDir + "/" + storedName)
                        .contentType(file.getContentType())
                        .size(file.getSize())
                        .issue(issue)
                        .uploadedBy(uploadedBy)
                        .message(message) // null si upload à la création de l'issue
                        .build();

                documents.add(document);

            } catch (IOException e) {
                throw new FileStorageException("Error storing file: " + file.getOriginalFilename(), e);
            }
        }

        return documentRepository.saveAll(documents);
    }

    private DocumentDto toDto(Document document) {
        return DocumentDto.builder()
                .id(document.getId())
                .originalName(document.getOriginalName())
                .contentType(document.getContentType())
                .size(document.getSize())
                .build();
    }
}
