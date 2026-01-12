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
     * @param tenantSlug    The tenant's slug for namespacing
     * @param attachmentId  The attachment ID for unique naming
     * @param file          The file to store
     * @return The storage key for retrieving the file later
     */
    String store(String tenantSlug, UUID attachmentId, MultipartFile file);

    /**
     * Load a file for download as an InputStream.
     *
     * @param storageKey The storage key returned from store()
     * @return InputStream for reading the file content
     */
    InputStream loadAsStream(String storageKey);
}
