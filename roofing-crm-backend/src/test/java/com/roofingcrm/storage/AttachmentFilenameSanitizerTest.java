package com.roofingcrm.storage;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("null")
class AttachmentFilenameSanitizerTest {

    private static final UUID ATT_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void sanitizeUploadedFilename_stripsPathSegmentsAndUnsafeCharacters() {
        assertEquals("passwd",
                AttachmentFilenameSanitizer.sanitizeUploadedFilename("../../../etc/passwd"));
        assertEquals("evil.dll",
                AttachmentFilenameSanitizer.sanitizeUploadedFilename("..\\..\\windows\\evil.dll"));
        assertEquals("a_b",
                AttachmentFilenameSanitizer.sanitizeUploadedFilename("a:b"));
        assertEquals("file",
                AttachmentFilenameSanitizer.sanitizeUploadedFilename("..."));
    }

    @Test
    void sanitizeTenantSlug_rejectsMultiSegmentAndTraversal() {
        assertThrows(IllegalArgumentException.class,
                () -> AttachmentFilenameSanitizer.sanitizeTenantSlug("../other"));
        assertThrows(IllegalArgumentException.class,
                () -> AttachmentFilenameSanitizer.sanitizeTenantSlug("a/b"));
        assertThrows(IllegalArgumentException.class,
                () -> AttachmentFilenameSanitizer.sanitizeTenantSlug(".."));
        assertEquals("acme-roofing", AttachmentFilenameSanitizer.sanitizeTenantSlug("acme-roofing "));
    }

    @Test
    void sanitizeForContentDisposition_removesControlCharacters() {
        assertEquals("doc.pdf",
                AttachmentFilenameSanitizer.sanitizeForContentDisposition("doc.pdf", ATT_ID));
        assertEquals("good.pdfSet-Cookie: a=b",
                AttachmentFilenameSanitizer.sanitizeForContentDisposition("good.pdf\r\nSet-Cookie: a=b", ATT_ID));
        assertEquals("evilhdr.pdf",
                AttachmentFilenameSanitizer.sanitizeForContentDisposition("evil\"hdr.pdf", ATT_ID));
        assertEquals(ATT_ID.toString(),
                AttachmentFilenameSanitizer.sanitizeForContentDisposition("\r\n", ATT_ID));
    }
}
