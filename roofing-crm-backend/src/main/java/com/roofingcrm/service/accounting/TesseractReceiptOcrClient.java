package com.roofingcrm.service.accounting;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Dual-path OCR: general full-page text vs numeric-focused summary crop (separate Tesseract configs).
 */
public class TesseractReceiptOcrClient implements ReceiptOcrClient {

    private static final Logger log = LoggerFactory.getLogger(TesseractReceiptOcrClient.class);

    private static final String NUMERIC_CHAR_WHITELIST =
            "0123456789.$,%:-/()ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz ";

    private final ITesseract textTesseract;
    private final ITesseract numericTesseract;

    public TesseractReceiptOcrClient(ReceiptOcrProperties properties) {
        this.textTesseract = createBaseTesseract(properties);
        textTesseract.setPageSegMode(1);
        textTesseract.setOcrEngineMode(1);

        this.numericTesseract = createBaseTesseract(properties);
        numericTesseract.setPageSegMode(6);
        numericTesseract.setOcrEngineMode(1);
        try {
            numericTesseract.setTessVariable("tessedit_char_whitelist", NUMERIC_CHAR_WHITELIST);
        } catch (Exception ex) {
            log.warn("Could not set Tesseract numeric whitelist: {}", ex.getMessage());
        }
    }

    private static Tesseract createBaseTesseract(ReceiptOcrProperties properties) {
        Tesseract tesseract = new Tesseract();
        String tessdata = trimToNull(properties.getTessdataPath());
        if (tessdata != null) {
            tesseract.setDatapath(tessdata);
        }
        String language = trimToNull(properties.getLanguage());
        tesseract.setLanguage(language != null ? language : "eng");
        return tesseract;
    }

    @Override
    public ReceiptOcrResult ocrFullAndSummary(
            BufferedImage fullPreprocessedImage,
            List<ReceiptSummaryRegionExtractor.SummaryRegionCrop> summaryCrops,
            ReceiptImagePreprocessor imagePreprocessor) {
        List<String> warnings = new ArrayList<>();
        String fullText = "";
        String summaryNumericText = "";
        try {
            fullText = safeTrim(textTesseract.doOCR(fullPreprocessedImage));
        } catch (TesseractException ex) {
            log.warn("Tesseract full-page (text) OCR failed: {}", ex.getMessage());
            warnings.add("Full-page OCR failed: " + abbreviate(ex.getMessage(), 200));
        }

        ReceiptSummaryRegionExtractor.SummaryRegionCrop tight = findCrop(summaryCrops, "tight");
        if (tight != null && imagePreprocessor != null) {
            try {
                BufferedImage numericPrepared = imagePreprocessor.preprocessSummaryForNumericOcr(tight.image());
                summaryNumericText = safeTrim(numericTesseract.doOCR(numericPrepared));
            } catch (TesseractException ex) {
                log.warn("Tesseract summary numeric OCR failed: {}", ex.getMessage());
                warnings.add("Summary numeric OCR failed: " + abbreviate(ex.getMessage(), 200));
            }
        } else {
            warnings.add("Summary numeric OCR skipped (no tight crop or preprocessor).");
        }

        log.debug("Tesseract dual OCR: textChars={}, summaryNumericChars={}", fullText.length(), summaryNumericText.length());
        return new ReceiptOcrResult(fullText, summaryNumericText, "TESSERACT", warnings);
    }

    private static ReceiptSummaryRegionExtractor.SummaryRegionCrop findCrop(
            List<ReceiptSummaryRegionExtractor.SummaryRegionCrop> summaryCrops,
            String id) {
        if (summaryCrops == null) {
            return null;
        }
        for (ReceiptSummaryRegionExtractor.SummaryRegionCrop crop : summaryCrops) {
            if (id.equals(crop.id())) {
                return crop;
            }
        }
        return null;
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static String abbreviate(String message, int max) {
        if (message == null) {
            return "";
        }
        String m = message.trim();
        return m.length() <= max ? m : m.substring(0, max) + "...";
    }
}
