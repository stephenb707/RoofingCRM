package com.roofingcrm.storage;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class S3ObjectKeyBuilderTest {

    @Test
    void buildObjectKey_includesTenantAttachmentAndSanitizedName() {
        UUID tenant = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID att = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String key = S3ObjectKeyBuilder.buildObjectKey(tenant, att, "photo.png", "");
        assertEquals("tenants/11111111-1111-1111-1111-111111111111/attachments/22222222-2222-2222-2222-222222222222/photo.png", key);
    }

    @Test
    void buildObjectKey_prefixNormalized() {
        UUID tenant = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID att = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String key = S3ObjectKeyBuilder.buildObjectKey(tenant, att, "a.pdf", " /my/prefix/ ");
        assertEquals("my/prefix/tenants/11111111-1111-1111-1111-111111111111/attachments/22222222-2222-2222-2222-222222222222/a.pdf", key);
    }

    @Test
    void assertKeyBelongsToTenant_rejectsEscape() {
        UUID tenant = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String other = "tenants/99999999-9999-9999-9999-999999999999/attachments/" + UUID.randomUUID() + "/x.png";
        assertThrows(IllegalArgumentException.class,
                () -> S3ObjectKeyBuilder.assertKeyBelongsToTenant(tenant, other, ""));
    }

    @Test
    void assertKeyBelongsToTenant_acceptsWithPrefix() {
        UUID tenant = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID att = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String key = S3ObjectKeyBuilder.buildObjectKey(tenant, att, "f.webp", "prod");
        assertDoesNotThrow(() -> S3ObjectKeyBuilder.assertKeyBelongsToTenant(tenant, key, "prod"));
    }
}
