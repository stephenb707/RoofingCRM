package com.roofingcrm.service.accounting;

import com.roofingcrm.domain.enums.JobCostCategory;

import java.math.BigDecimal;

public interface ReceiptExtractionClient {

    ExtractedReceiptData extract(ReceiptVisionDocument document);

    ExtractedReceiptData extractSummary(ReceiptVisionDocument document);

    /**
     * Text-only interpretation (vendor, date, category, notes). Numeric totals must come from OCR / reconciliation, not from this call.
     */
    ExtractedReceiptData interpretFromTranscribedText(
            String fullOcrText,
            String summaryOcrText,
            ReceiptTextInterpretationContext context
    );

    record ReceiptVisionDocument(
            String attemptLabel,
            String fileName,
            String contentType,
            String promptContext,
            String imageMimeType,
            String imageBase64,
            Integer width,
            Integer height,
            Integer imageByteSize
    ) {
    }

    record ExtractedReceiptData(
            String vendorName,
            String incurredDate,
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal total,
            BigDecimal amountPaid,
            BigDecimal suggestedAmount,
            JobCostCategory suggestedCategory,
            String notes,
            Integer confidence,
            String rawExtractedText
    ) {
    }
}
