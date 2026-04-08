package com.roofingcrm.service.accounting;

import java.awt.image.BufferedImage;
import java.util.List;

public class NoopReceiptOcrClient implements ReceiptOcrClient {

    private final String reason;

    public NoopReceiptOcrClient(String reason) {
        this.reason = reason;
    }

    @Override
    public ReceiptOcrResult ocrFullAndSummary(
            BufferedImage fullPreprocessedImage,
            List<ReceiptSummaryRegionExtractor.SummaryRegionCrop> summaryCrops,
            ReceiptImagePreprocessor imagePreprocessor) {
        return ReceiptOcrResult.empty("NOOP", reason);
    }
}
