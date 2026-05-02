package com.roofingcrm.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SafeDownloadPdfFilenameTest {

    @Test
    void fromReportTitle_trimsWhitespaceAndAppendsPdf() {
        assertEquals("Roof Inspection Report.pdf", SafeDownloadPdfFilename.fromReportTitle("  Roof Inspection Report  "));
    }

    @Test
    void fromReportTitle_replacesUnsafeCharactersAndStripsExtension() {
        assertEquals(
                "Before - and After Report - Smith-Home.pdf",
                SafeDownloadPdfFilename.fromReportTitle("Before <> and After Report : Smith/Home"));
        assertEquals("a-b.pdf", SafeDownloadPdfFilename.fromReportTitle("a/b"));
    }

    @Test
    void fromReportTitle_stripsRedundantPdfSuffix() {
        assertEquals("My Report.pdf", SafeDownloadPdfFilename.fromReportTitle("My Report.pdf"));
    }

    @Test
    void fromReportTitle_fallbackWhenBlank() {
        assertEquals("Customer Report.pdf", SafeDownloadPdfFilename.fromReportTitle("   "));
    }

    @Test
    void fromReportTitle_truncatesVeryLongStem() {
        String longTitle = "A".repeat(300);
        String name = SafeDownloadPdfFilename.fromReportTitle(longTitle);
        assertFalse(name.contains(" "));
        assertEquals(184, name.length());
        assertEquals(".pdf", name.substring(name.length() - 4));
    }
}
