package com.fan.mixmyfit.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StoredFileService {
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final Path uploadRoot;

    public StoredFileService(@Value("${UPLOAD_DIR:backend/uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file) {
        validate(file);
        String contentType = file.getContentType();
        String serverFilename = UUID.randomUUID() + EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
        Path destination = uploadRoot.resolve(serverFilename).normalize();
        if (!destination.startsWith(uploadRoot)) {
            throw new FileException("UPLOAD_STORAGE_FAILED", "Could not store upload");
        }
        try {
            Files.createDirectories(uploadRoot);
            file.transferTo(destination);
        } catch (IOException exception) {
            throw new FileException("UPLOAD_STORAGE_FAILED", "Could not store upload");
        }
        return new StoredFile(
                destination.toString(),
                file.getOriginalFilename(),
                contentType,
                file.getSize());
    }

    public byte[] read(String imagePath) {
        Path stored = Path.of(imagePath).toAbsolutePath().normalize();
        if (!stored.startsWith(uploadRoot)) {
            throw new FileException("UPLOAD_FILE_NOT_FOUND", "File not found");
        }
        try {
            return Files.readAllBytes(stored);
        } catch (IOException exception) {
            throw new FileException("UPLOAD_FILE_NOT_FOUND", "File not found");
        }
    }

    private static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileException("UPLOAD_FILE_REQUIRED", "File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileException("UPLOAD_FILE_TOO_LARGE", "File exceeds 5 MB limit");
        }
        if (!EXTENSIONS_BY_CONTENT_TYPE.containsKey(file.getContentType())) {
            throw new FileException("UPLOAD_CONTENT_TYPE_NOT_ALLOWED", "Content type is not allowed");
        }
    }
}
