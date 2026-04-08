package com.roofingcrm.service.accounting;

import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.util.List;

@Component
public class ReceiptImagePreprocessor {

    private static final int TARGET_MIN_WIDTH = 1400;
    private static final int TARGET_MAX_WIDTH = 2200;
    private static final float CONTRAST_SCALE = 1.28f;
    private static final float CONTRAST_OFFSET = -12f;
    /**
     * Vision API summary pass: strong upscale + contrast + sharpen so small digits are readable.
     * Target width is ~3× source (min/max clamp); bicubic interpolation for upscaling.
     */
    private static final int SUMMARY_VISION_MIN_WIDTH = 2800;
    private static final int SUMMARY_VISION_MAX_WIDTH = 3600;
    private static final float VISION_CONTRAST_SCALE = 1.48f;
    private static final float VISION_CONTRAST_OFFSET = -18f;
    /** Soft binarization for the "threshold" variant only (milder than legacy 170 cutoff). */
    private static final int VISION_SOFT_THRESHOLD_CUTOFF = 198;
    /** Upscaled summary crop for numeric-only OCR (totals block). */
    private static final int SUMMARY_NUMERIC_TARGET_WIDTH = 2400;
    private static final int SUMMARY_NUMERIC_MAX_WIDTH = 3200;

    public ProcessedReceiptImage preprocess(BufferedImage source) {
        BufferedImage grayscale = toGrayscale(source);
        BufferedImage contrasted = increaseContrast(grayscale);
        BufferedImage resized = resizeForReadability(contrasted);
        return new ProcessedReceiptImage(source.getWidth(), source.getHeight(), resized);
    }

    public List<SummaryImageVariant> preprocessSummaryVariants(BufferedImage source) {
        return List.of(
                preprocessSummaryVariant(source, "baseline"),
                preprocessSummaryVariant(source, "threshold"),
                preprocessSummaryVariant(source, "sharpened")
        );
    }

    /**
     * Single dedicated pipeline for OpenAI vision on summary crops (one image per call; variants add light differences).
     * Grayscale → contrast → bicubic upscale (~3× width, clamped) → sharpen.
     */
    public BufferedImage preprocessSummaryForVision(BufferedImage source) {
        BufferedImage grayscale = toGrayscale(source);
        BufferedImage contrasted = increaseContrastForVision(grayscale);
        int targetWidth = visionTargetWidth(contrasted.getWidth());
        BufferedImage scaled = resizeToWidthBicubic(contrasted, targetWidth);
        return sharpen(scaled);
    }

    /**
     * Strong preprocessing for the summary/totals crop only: upscale, grayscale, contrast, threshold, sharpen.
     * Intended for Tesseract numeric mode — separate from full-page text OCR.
     */
    public BufferedImage preprocessSummaryForNumericOcr(BufferedImage source) {
        BufferedImage grayscale = toGrayscale(source);
        BufferedImage strong = increaseContrastStrong(grayscale);
        int target = Math.min(SUMMARY_NUMERIC_MAX_WIDTH, Math.max(SUMMARY_NUMERIC_TARGET_WIDTH, strong.getWidth() * 2));
        BufferedImage scaled = resizeToWidth(strong, target);
        BufferedImage thresholded = applyThreshold(scaled);
        return sharpen(thresholded);
    }

    public SummaryImageVariant preprocessSummaryVariant(BufferedImage source, String variantId) {
        BufferedImage vision = preprocessSummaryForVision(source);
        return switch (variantId) {
            case "baseline" -> new SummaryImageVariant("baseline", 3, vision);
            case "threshold" -> new SummaryImageVariant("threshold", 2, applySoftThresholdForVision(vision));
            case "sharpened" -> new SummaryImageVariant("sharpened", 2, sharpen(vision));
            default -> throw new IllegalArgumentException("Unsupported summary preprocessing variant: " + variantId);
        };
    }

    private static int visionTargetWidth(int sourceWidth) {
        int triple = (int) Math.round(sourceWidth * 3.0d);
        return Math.min(SUMMARY_VISION_MAX_WIDTH, Math.max(SUMMARY_VISION_MIN_WIDTH, triple));
    }

    private BufferedImage increaseContrastForVision(BufferedImage source) {
        BufferedImage contrasted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        RescaleOp op = new RescaleOp(VISION_CONTRAST_SCALE, VISION_CONTRAST_OFFSET, null);
        op.filter(source, contrasted);
        return contrasted;
    }

    /**
     * Mild binarization after vision pipeline — can help faint ink without harsh 170 threshold.
     */
    private BufferedImage applySoftThresholdForVision(BufferedImage source) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int rgb = source.getRGB(x, y) & 0xFF;
                int value = rgb > VISION_SOFT_THRESHOLD_CUTOFF ? 0xFFFFFF : 0x000000;
                out.setRGB(x, y, value);
            }
        }
        return out;
    }

    private BufferedImage resizeToWidthBicubic(BufferedImage source, int targetWidth) {
        if (targetWidth == source.getWidth()) {
            return source;
        }
        int targetHeight = Math.max(1, (int) Math.round((double) source.getHeight() * ((double) targetWidth / (double) source.getWidth())));
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setBackground(Color.WHITE);
            graphics.clearRect(0, 0, targetWidth, targetHeight);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private BufferedImage toGrayscale(BufferedImage source) {
        BufferedImage grayscale = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = grayscale.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return grayscale;
    }

    private BufferedImage increaseContrast(BufferedImage source) {
        BufferedImage contrasted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        RescaleOp op = new RescaleOp(CONTRAST_SCALE, CONTRAST_OFFSET, null);
        op.filter(source, contrasted);
        return contrasted;
    }

    private BufferedImage increaseContrastStrong(BufferedImage source) {
        BufferedImage contrasted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        RescaleOp op = new RescaleOp(1.55f, -20f, null);
        op.filter(source, contrasted);
        return contrasted;
    }

    private BufferedImage applyThreshold(BufferedImage source) {
        BufferedImage thresholded = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics graphics = thresholded.getGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        for (int y = 0; y < thresholded.getHeight(); y++) {
            for (int x = 0; x < thresholded.getWidth(); x++) {
                int rgb = thresholded.getRGB(x, y) & 0xFF;
                int value = rgb > 170 ? 0xFFFFFF : 0x000000;
                thresholded.setRGB(x, y, value);
            }
        }
        return thresholded;
    }

    private BufferedImage sharpen(BufferedImage source) {
        float[] kernel = {
                0f, -1f, 0f,
                -1f, 5f, -1f,
                0f, -1f, 0f
        };
        ConvolveOp op = new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(source, null);
    }

    private BufferedImage resizeForReadability(BufferedImage source) {
        int width = source.getWidth();
        int targetWidth = width;

        if (width < TARGET_MIN_WIDTH) {
            targetWidth = TARGET_MIN_WIDTH;
        } else if (width > TARGET_MAX_WIDTH) {
            targetWidth = TARGET_MAX_WIDTH;
        }

        if (targetWidth == width) {
            return source;
        }

        return resizeToWidth(source, targetWidth);
    }

    private BufferedImage resizeToWidth(BufferedImage source, int targetWidth) {
        if (targetWidth == source.getWidth()) {
            return source;
        }

        int targetHeight = Math.max(1, (int) Math.round((double) source.getHeight() * ((double) targetWidth / (double) source.getWidth())));
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setBackground(Color.WHITE);
            graphics.clearRect(0, 0, targetWidth, targetHeight);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    public record ProcessedReceiptImage(
            int originalWidth,
            int originalHeight,
            BufferedImage image
    ) {
    }

    public record SummaryImageVariant(
            String id,
            int weight,
            BufferedImage image
    ) {
    }
}
