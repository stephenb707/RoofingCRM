package com.roofingcrm.service.accounting;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptImagePreprocessorTest {

    private final ReceiptImagePreprocessor preprocessor = new ReceiptImagePreprocessor();

    @Test
    void preprocess_returnsNormalizedReadableImage() {
        BufferedImage source = new BufferedImage(600, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.drawString("TOTAL 1564.38", 200, 820);
        } finally {
            graphics.dispose();
        }

        ReceiptImagePreprocessor.ProcessedReceiptImage result = preprocessor.preprocess(source);

        assertEquals(600, result.originalWidth());
        assertEquals(900, result.originalHeight());
        assertNotNull(result.image());
        assertTrue(result.image().getWidth() >= 1400);
        assertTrue(result.image().getHeight() > source.getHeight());
    }

    @Test
    void preprocessSummaryVariants_returnsMultipleReadableVariants() {
        BufferedImage source = new BufferedImage(500, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.drawString("SUBTOTAL 1455.24", 20, 120);
            graphics.drawString("TAX 109.14", 20, 150);
            graphics.drawString("TOTAL 1564.38", 20, 180);
        } finally {
            graphics.dispose();
        }

        List<ReceiptImagePreprocessor.SummaryImageVariant> variants = preprocessor.preprocessSummaryVariants(source);

        assertEquals(3, variants.size());
        assertEquals("baseline", variants.get(0).id());
        assertEquals("threshold", variants.get(1).id());
        assertEquals("sharpened", variants.get(2).id());
        assertTrue(variants.stream().allMatch(variant -> variant.image().getWidth() >= 2700));
    }

    @Test
    void preprocessSummaryForVision_upscalesSmallCropAndReturnsNonEmptyImage() {
        BufferedImage source = new BufferedImage(320, 160, BufferedImage.TYPE_INT_RGB);
        var graphics = source.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
            graphics.setColor(java.awt.Color.BLACK);
            graphics.drawString("TOTAL 1564.38", 20, 100);
        } finally {
            graphics.dispose();
        }

        BufferedImage vision = preprocessor.preprocessSummaryForVision(source);

        assertNotNull(vision);
        assertTrue(vision.getWidth() >= 2700);
        assertTrue(vision.getHeight() > 0);
    }
}
