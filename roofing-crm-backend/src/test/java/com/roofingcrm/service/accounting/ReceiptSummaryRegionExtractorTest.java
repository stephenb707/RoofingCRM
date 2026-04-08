package com.roofingcrm.service.accounting;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReceiptSummaryRegionExtractorTest {

    private final ReceiptSummaryRegionExtractor extractor = new ReceiptSummaryRegionExtractor();

    @Test
    void extractVariants_returnsMultipleBottomRightCropGeometries() {
        BufferedImage source = new BufferedImage(2000, 1000, BufferedImage.TYPE_BYTE_GRAY);

        List<ReceiptSummaryRegionExtractor.SummaryRegionCrop> crops = extractor.extractVariants(source);

        assertEquals(3, crops.size());

        ReceiptSummaryRegionExtractor.SummaryRegionCrop tight = crops.get(0);
        assertEquals("tight", tight.id());
        assertEquals(1040, tight.x());
        assertEquals(480, tight.y());
        assertEquals(960, tight.width());
        assertEquals(440, tight.height());

        ReceiptSummaryRegionExtractor.SummaryRegionCrop large = crops.get(1);
        assertEquals("large", large.id());
        assertEquals(840, large.x());
        assertEquals(410, large.y());
        assertEquals(1160, large.width());
        assertEquals(540, large.height());

        ReceiptSummaryRegionExtractor.SummaryRegionCrop expanded = crops.get(2);
        assertEquals("expanded", expanded.id());
        assertEquals(1080, expanded.width());
        assertEquals(500, expanded.height());
        assertEquals(840, expanded.x());
        assertEquals(420, expanded.y());
    }
}
