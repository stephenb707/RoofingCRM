package com.roofingcrm.service.customerreport;

import com.roofingcrm.domain.entity.Attachment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportGalleryImageMimeTest {

    @Test
    void primaryType_stripsParametersAndLowercases() {
        assertEquals("image/jpeg", ReportGalleryImageMime.primaryType("  Image/JPEG ; charset=UTF-8 "));
    }

    @Test
    void isSupported_acceptsCommonRasterTypes() {
        assertTrue(ReportGalleryImageMime.isSupported("image/png"));
        assertTrue(ReportGalleryImageMime.isSupported("image/jpeg"));
        assertTrue(ReportGalleryImageMime.isSupported("image/jpg"));
        assertTrue(ReportGalleryImageMime.isSupported("image/pjpeg"));
        assertTrue(ReportGalleryImageMime.isSupported("image/gif"));
        assertTrue(ReportGalleryImageMime.isSupported("image/webp"));
        assertTrue(ReportGalleryImageMime.isSupported("image/bmp"));
        assertTrue(ReportGalleryImageMime.isSupported("image/x-ms-bmp"));
        assertTrue(ReportGalleryImageMime.isSupported("image/tiff"));
        assertTrue(ReportGalleryImageMime.isSupported("image/tif"));
        assertTrue(ReportGalleryImageMime.isSupported("image/x-tiff"));
    }

    @Test
    void isSupported_rejectsExoticImageSubtypesUntilExplicitlyBacked() {
        assertFalse(ReportGalleryImageMime.isSupported("image/svg+xml"));
        assertFalse(ReportGalleryImageMime.isSupported("image/heic"));
        assertFalse(ReportGalleryImageMime.isSupported("application/pdf"));
    }

    @Test
    void requireSupported_throwsOnNullAttachment() {
        assertThrows(NullPointerException.class,
                () -> ReportGalleryImageMime.requireSupportedReportGalleryImage(null));
    }

    @Test
    void requireSupported_throwsMeaningfully() {
        Attachment att = new Attachment();
        att.setContentType("image/svg+xml");
        assertThrows(IllegalArgumentException.class, () -> ReportGalleryImageMime.requireSupportedReportGalleryImage(att));
    }
}
