package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.StorageSpaceFullException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class FileStorageService {

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Value("${app.upload-max-total-bytes:1073741824}") // 1 Go par défaut
    private long maxTotalBytes;

    public String store(MultipartFile file) throws IOException {

        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);

        // Espace max du dossier cible
        if (currentDirSize(dir) + file.getSize() > maxTotalBytes) {
            throw new StorageSpaceFullException();
        }

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "." + extension;

        Path target = dir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return storedName;
    }

    private long currentDirSize(Path dir) throws IOException {
        if (!Files.exists(dir)) return 0;
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(Files::isRegularFile)
                    .mapToLong(p -> p.toFile().length())
                    .sum();
        }
    }
}
