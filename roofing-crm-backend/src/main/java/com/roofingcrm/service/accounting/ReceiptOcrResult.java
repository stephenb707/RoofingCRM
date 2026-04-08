package com.roofingcrm.service.accounting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Raw OCR output for a receipt: full page and focused summary region.
 */
public record ReceiptOcrResult(
        String fullText,
        String summaryText,
        String providerId,
        List<String> warnings
) {
    public ReceiptOcrResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public boolean hasUsableFullText(int minChars) {
        return fullText != null && fullText.trim().length() >= minChars;
    }

    public static ReceiptOcrResult empty(String providerId, String reason) {
        List<String> w = new ArrayList<>();
        if (reason != null && !reason.isBlank()) {
            w.add(reason);
        }
        return new ReceiptOcrResult("", "", providerId, w);
    }

    public static ReceiptOcrResult mergePdfAndOcr(String pdfEmbeddedText, ReceiptOcrResult ocr) {
        String pdf = pdfEmbeddedText == null ? "" : pdfEmbeddedText.trim();
        String ocrFull = ocr.fullText() == null ? "" : ocr.fullText().trim();
        String combined;
        if (pdf.isEmpty()) {
            combined = ocrFull;
        } else if (ocrFull.isEmpty()) {
            combined = pdf;
        } else {
            combined = pdf + "\n\n--- OCR ---\n\n" + ocrFull;
        }
        List<String> w = new ArrayList<>(ocr.warnings());
        return new ReceiptOcrResult(combined, ocr.summaryText(), ocr.providerId(), Collections.unmodifiableList(w));
    }
}
