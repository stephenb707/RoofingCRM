package com.roofingcrm.storage;

import java.util.Objects;
import java.util.UUID;

/**
 * Builds and validates tenant-scoped object keys for S3-compatible storage.
 */
public final class S3ObjectKeyBuilder {

    private S3ObjectKeyBuilder() {
    }

    public static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String p = prefix.trim().replace('\\', '/');
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    public static String buildObjectKey(UUID tenantId, UUID attachmentId, String safeFilename, String rawPrefix) {
        String prefix = normalizePrefix(rawPrefix);
        String core = "tenants/%s/attachments/%s/%s"
                .formatted(Objects.requireNonNull(tenantId), Objects.requireNonNull(attachmentId), safeFilename);
        return prefix.isEmpty() ? core : prefix + "/" + core;
    }

    /**
     * Ensures {@code storageKey} is under the expected tenant prefix (prevents cross-tenant reads given a forged key).
     */
    public static void assertKeyBelongsToTenant(UUID tenantId, String storageKey, String rawPrefix) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        String prefix = normalizePrefix(rawPrefix);
        String expectedRoot = prefix.isEmpty()
                ? "tenants/%s/attachments/".formatted(tenantId)
                : prefix + "/tenants/%s/attachments/".formatted(tenantId);
        String normalizedKey = storageKey.replace('\\', '/');
        if (!normalizedKey.startsWith(expectedRoot)) {
            throw new IllegalArgumentException("Storage key does not belong to tenant");
        }
    }
}
