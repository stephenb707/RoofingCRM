package com.roofingcrm.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * Storage abstraction for file attachments.
 * Implementations can store files locally or in cloud storage (e.g., S3).
 */
public interface AttachmentStorageService {

    /**
     * Store a file and return a storage key (path or external key).
     *
     * @param tenantId     Tenant id; used for S3 key isolation
     * @param tenantSlug   Tenant slug; used for local on-disk layout
     * @param attachmentId The attachment ID for unique naming
     * @param file         The file to store
     * @return The storage key for retrieving the file later
     */
    String store(UUID tenantId, String tenantSlug, UUID attachmentId, MultipartFile file);

    /**
     * Load a file for download as an InputStream.
     *
     * @param tenantId   Tenant id (S3 isolation); for local storage, {@code tenantSlug} scopes the key
     * @param tenantSlug Tenant slug string as returned by {@code Tenant#getSlug()} or id fallback — unused for S3
     * @param storageKey The storage key returned from store()
     * @return InputStream for reading the file content
     */
    InputStream loadAsStream(UUID tenantId, String tenantSlug, String storageKey);
}
