package com.roofingcrm.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local filesystem implementation of AttachmentStorageService.
 * Stores files under a configurable base directory, organized by tenant.
 */
@Service
public class LocalAttachmentStorageService implements AttachmentStorageService {

    private final LocalStorageProperties properties;

    public LocalAttachmentStorageService(LocalStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public String store(String tenantSlug, UUID attachmentId, MultipartFile file) {
        try {
            Path baseDir = Path.of(properties.getBaseDir()).toAbsolutePath().normalize();
            Path tenantDir = baseDir.resolve(tenantSlug);
            Files.createDirectories(tenantDir);

            String originalFilename = file.getOriginalFilename();
            String safeName = (originalFilename != null && !originalFilename.isBlank())
                    ? originalFilename
                    : attachmentId.toString();

            // Use attachment ID prefix to ensure uniqueness
            String fileName = attachmentId.toString() + "_" + safeName;
            Path target = tenantDir.resolve(fileName);
            
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // storageKey is a relative path under baseDir
            return tenantSlug + "/" + fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store attachment", ex);
        }
    }

    @Override
    public InputStream loadAsStream(String storageKey) {
        try {
            Path baseDir = Path.of(properties.getBaseDir()).toAbsolutePath().normalize();
            Path path = baseDir.resolve(storageKey).normalize();
            
            if (!Files.exists(path)) {
                throw new IOException("File not found: " + storageKey);
            }
            
            return Files.newInputStream(path);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load attachment", ex);
        }
    }
}
