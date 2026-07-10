package com.roofingcrm.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * S3-compatible object storage for attachments. Files are private; download stays on the backend.
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3AttachmentStorageService implements AttachmentStorageService {

    private final S3Client s3Client;
    private final S3AttachmentStorageProperties props;

    public S3AttachmentStorageService(S3Client s3Client, S3AttachmentStorageProperties props) {
        this.s3Client = s3Client;
        this.props = props;
    }

    @Override
    public String store(UUID tenantId, String tenantSlug, UUID attachmentId, MultipartFile file) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(attachmentId);
        Objects.requireNonNull(file);
        String safeName = AttachmentFilenameSanitizer.sanitizeUploadedFilename(file.getOriginalFilename());
        String key = S3ObjectKeyBuilder.buildObjectKey(tenantId, attachmentId, safeName, props.getPrefix());

        String disposition = ContentDisposition.attachment()
                .filename(safeName, StandardCharsets.UTF_8)
                .build()
                .toString();

        try (InputStream in = file.getInputStream()) {
            PutObjectRequest.Builder b = PutObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .contentDisposition(disposition);
            s3Client.putObject(b.build(), RequestBody.fromInputStream(in, file.getSize()));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to store attachment in object storage", ex);
        }
        return key;
    }

    @Override
    public InputStream loadAsStream(UUID tenantId, String tenantSlug, String storageKey) {
        Objects.requireNonNull(tenantId);
        S3ObjectKeyBuilder.assertKeyBelongsToTenant(tenantId, storageKey, props.getPrefix());
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(props.getBucket())
                .key(storageKey)
                .build();
        try {
            return s3Client.getObject(req);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load attachment from object storage", ex);
        }
    }
}
