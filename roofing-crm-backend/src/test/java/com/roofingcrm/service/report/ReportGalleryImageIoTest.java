package com.roofingcrm.service.report;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ensures TwelveMonkeys ImageIO SPIs are present so {@link com.roofingcrm.service.customerreport.ReportGalleryImageMime}
 * stays aligned with PDF embedding capability.
 */
class ReportGalleryImageIoTest {

    @Test
    void twelveMonkeys_registersWebpAndTiffReaders() {
        assertTrue(Arrays.stream(ImageIO.getReaderFormatNames()).anyMatch(f -> f.equalsIgnoreCase("webp")),
                "Expected WebP ImageIO reader (com.twelvemonkeys.imageio:imageio-webp)");
        assertTrue(Arrays.stream(ImageIO.getReaderFormatNames()).anyMatch(f -> f.equalsIgnoreCase("tiff") || f.equalsIgnoreCase("tif")),
                "Expected TIFF ImageIO reader (com.twelvemonkeys.imageio:imageio-tiff)");
    }
}
