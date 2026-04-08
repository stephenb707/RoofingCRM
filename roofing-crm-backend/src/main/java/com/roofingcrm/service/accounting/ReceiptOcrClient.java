package com.roofingcrm.service.accounting;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Runs OCR on a preprocessed receipt image and an optional summary crop.
 */
public interface ReceiptOcrClient {

    ReceiptOcrResult ocrFullAndSummary(
            BufferedImage fullPreprocessedImage,
            List<ReceiptSummaryRegionExtractor.SummaryRegionCrop> summaryCrops,
            ReceiptImagePreprocessor imagePreprocessor
    );
}
