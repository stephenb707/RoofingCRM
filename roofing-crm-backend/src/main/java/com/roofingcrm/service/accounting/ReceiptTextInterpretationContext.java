package com.roofingcrm.service.accounting;

/**
 * Metadata for OCR-first text interpretation (vendor/date/category/notes) without vision.
 */
public record ReceiptTextInterpretationContext(
        String fileName,
        String contentType,
        String promptContext
) {
}
