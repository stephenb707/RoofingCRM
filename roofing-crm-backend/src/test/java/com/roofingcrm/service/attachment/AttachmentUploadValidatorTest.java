package com.roofingcrm.service.attachment;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.util.List;

import static com.roofingcrm.service.attachment.UploadValidationTestFixtures.MINIMAL_JPEG_BYTES;
import static com.roofingcrm.service.attachment.UploadValidationTestFixtures.MINIMAL_PDF_BYTES;
import static com.roofingcrm.service.attachment.UploadValidationTestFixtures.MINIMAL_PNG_BYTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachmentUploadValidatorTest {

    @Test
    void isAllowedContentType_matchesExactAndWildcard() {
        List<String> allowed = List.of("application/pdf", "image/*");
        assertTrue(AttachmentUploadValidator.isAllowedContentType("application/pdf", allowed));
        assertTrue(AttachmentUploadValidator.isAllowedContentType("image/png", allowed));
        assertTrue(AttachmentUploadValidator.isAllowedContentType("IMAGE/JPEG", allowed));
        assertFalse(AttachmentUploadValidator.isAllowedContentType("video/mp4", allowed));
        assertFalse(AttachmentUploadValidator.isAllowedContentType(null, allowed));
        assertFalse(AttachmentUploadValidator.isAllowedContentType("  ", allowed));
    }

    @Test
    void validate_acceptsPdfForReceipt() {
        AttachmentUploadProperties props = new AttachmentUploadProperties();
        AttachmentUploadValidator v = new AttachmentUploadValidator(props);
        MockMultipartFile file =
                new MockMultipartFile("file", "r.pdf", "application/pdf", MINIMAL_PDF_BYTES);
        v.validate(file, AttachmentUploadContext.JOB_RECEIPT);
    }

    @Test
    void validate_acceptsValidPngForLead() {
        AttachmentUploadProperties props = new AttachmentUploadProperties();
        AttachmentUploadValidator v = new AttachmentUploadValidator(props);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", MINIMAL_PNG_BYTES);
        v.validate(file, AttachmentUploadContext.LEAD_ATTACHMENT);
    }

    @Test
    void validate_acceptsValidJpegForJob() {
        AttachmentUploadProperties props = new AttachmentUploadProperties();
        AttachmentUploadValidator v = new AttachmentUploadValidator(props);
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", MINIMAL_JPEG_BYTES);
        v.validate(file, AttachmentUploadContext.JOB_ATTACHMENT);
    }

    @Test
    void validate_rejectsFakeImageBytes() {
        AttachmentUploadProperties props = new AttachmentUploadProperties();
        AttachmentUploadValidator v = new AttachmentUploadValidator(props);
        MockMultipartFile file =
                new MockMultipartFile("file", "x.jpg", "image/jpeg", "not-a-real-image".getBytes());
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> v.validate(file, AttachmentUploadContext.JOB_ATTACHMENT));
        assertTrue(ex.getMessage().contains("image could not be read"));
    }

    @Test
    void validate_rejectsPdfWithoutMagicBytes() {
        AttachmentUploadProperties props = new AttachmentUploadProperties();
        AttachmentUploadValidator v = new AttachmentUploadValidator(props);
        MockMultipartFile file =
                new MockMultipartFile("file", "x.pdf", "application/pdf", "just text".getBytes());
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> v.validate(file, AttachmentUploadContext.JOB_RECEIPT));
        assertTrue(ex.getMessage().contains("does not match its declared type"));
    }

    @Test
    void validate_rejectsRasterDeclaredAsPdf() {
        AttachmentUploadProperties props = new AttachmentUploadProperties();
        AttachmentUploadValidator v = new AttachmentUploadValidator(props);
        MockMultipartFile file =
                new MockMultipartFile("file", "x.pdf", "application/pdf", MINIMAL_PNG_BYTES);
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> v.validate(file, AttachmentUploadContext.JOB_RECEIPT));
        assertTrue(ex.getMessage().contains("does not match its declared type"));
    }

    @Test
    void validate_whenContentTypesNotEnforced_skipsContentSniff() {
        AttachmentUploadProperties props = new AttachmentUploadProperties();
        props.setEnforceContentTypes(false);
        AttachmentUploadValidator v = new AttachmentUploadValidator(props);
        MockMultipartFile file =
                new MockMultipartFile("file", "x.jpg", "image/jpeg", "garbage".getBytes());
        v.validate(file, AttachmentUploadContext.LEAD_ATTACHMENT);
    }

    @Test
    void maxFileSizeDisplay_prefersWholeBinaryUnits() {
        AttachmentUploadProperties p = new AttachmentUploadProperties();
        assertEquals("20MB", p.getMaxFileSizeDisplayString());
        p.setMaxFileSize(DataSize.ofKilobytes(512));
        assertEquals("512KB", p.getMaxFileSizeDisplayString());
        p.setMaxFileSize(DataSize.ofGigabytes(2));
        assertEquals("2GB", p.getMaxFileSizeDisplayString());
    }

    @Test
    void maxFileSizeDisplay_fallsBackToBytesWhenNonAligned() {
        AttachmentUploadProperties p = new AttachmentUploadProperties();
        p.setMaxFileSize(DataSize.ofBytes(1000));
        assertEquals("1000 bytes", p.getMaxFileSizeDisplayString());
        p.setMaxFileSize(DataSize.ofBytes(1));
        assertEquals("1 byte", p.getMaxFileSizeDisplayString());
    }
}
