package fr.horizon.backend_api.service;

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

@Service
public class FileStorageService {

    @Value("${app.upload-dir}")
    private String uploadDir;

    public String store(MultipartFile file) throws IOException {

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());

        String storedName = UUID.randomUUID() + "." + extension;

        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);

        Path target = dir.resolve(storedName);

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return storedName;
    }
}
