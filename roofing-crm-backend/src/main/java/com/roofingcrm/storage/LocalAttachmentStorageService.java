package com.roofingcrm.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Local filesystem implementation of AttachmentStorageService.
 * Stores files under a configurable base directory, organized by tenant.
 * <p>
 * Switching to S3 does not migrate existing local files; use S3 in production before collecting
 * customer uploads, or add a dedicated migration utility later.
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalAttachmentStorageService implements AttachmentStorageService {

    private final LocalStorageProperties properties;

    public LocalAttachmentStorageService(LocalStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public String store(UUID tenantId, String tenantSlug, UUID attachmentId, MultipartFile file) {
        try {
            Path baseDir = Path.of(properties.getBaseDir()).toAbsolutePath().normalize();

            Objects.requireNonNull(tenantId, "tenantId");
            String safeTenant = AttachmentFilenameSanitizer.sanitizeTenantSlug(tenantSlug);
            Path tenantDir = resolveStrictlyUnderBase(baseDir, safeTenant);
            Files.createDirectories(tenantDir);

            String safeBasename = AttachmentFilenameSanitizer.sanitizeUploadedFilename(file.getOriginalFilename());
            String fileName = attachmentId + "_" + safeBasename;
            Path target = resolveStrictlyUnderBase(tenantDir, fileName);

            try (InputStream is = file.getInputStream()) {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // storageKey is a relative path under baseDir
            return safeTenant + "/" + fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store attachment", ex);
        }
    }

    @Override
    public InputStream loadAsStream(UUID tenantId, String tenantSlug, String storageKey) {
        try {
            Objects.requireNonNull(tenantId, "tenantId");
            assertLocalKeyForTenant(tenantSlug, storageKey);
            Path baseDir = Path.of(properties.getBaseDir()).toAbsolutePath().normalize();
            Path path = resolveStrictlyUnderBase(baseDir, storageKey);

            if (!Files.exists(path)) {
                throw new IOException("File not found: " + storageKey);
            }

            return Files.newInputStream(path);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load attachment", ex);
        }
    }

    /**
     * Local keys are relative paths under ./{slug}/...; the slug segment must match the tenant namespace.
     */
    static void assertLocalKeyForTenant(String tenantSlug, String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        if (tenantSlug == null || tenantSlug.isBlank()) {
            throw new IllegalArgumentException("tenantSlug required for local storage key verification");
        }
        String expectedFolder = AttachmentFilenameSanitizer.sanitizeTenantSlug(tenantSlug.trim());
        if (!storageKey.startsWith(expectedFolder + "/")) {
            throw new IllegalArgumentException("Storage key does not belong to tenant");
        }
    }

    /**
     * Resolves {@code relativeKey} under {@code baseDir}, rejecting absolute keys and any normalized path
     * that escapes {@code baseDir}.
     */
    static Path resolveStrictlyUnderBase(Path baseDir, String relativeKey) {
        if (relativeKey == null || relativeKey.isBlank()) {
            throw new IllegalArgumentException("Invalid path key");
        }
        Path keyPath = Path.of(relativeKey);
        if (keyPath.isAbsolute()) {
            throw new IllegalArgumentException("Invalid path key");
        }
        Path resolved = baseDir.resolve(keyPath).normalize().toAbsolutePath();
        if (!resolved.startsWith(baseDir)) {
            throw new IllegalArgumentException("Invalid path key");
        }
        return resolved;
    }
}
