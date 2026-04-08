package com.roofingcrm.service.accounting;

import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.enums.JobCostCategory;
import com.roofingcrm.domain.enums.ReceiptAmountConfidence;
import com.roofingcrm.domain.enums.ReceiptFieldConfidence;
import com.roofingcrm.domain.enums.ReceiptExtractionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface ReceiptExtractionService {

    ExtractionDraft extractReceipt(Attachment receipt);

    record ExtractionDraft(
            ReceiptExtractionStatus status,
            Instant extractedAt,
            String error,
            String vendorName,
            Instant incurredAt,
            BigDecimal amount,
            BigDecimal extractedSubtotal,
            BigDecimal extractedTax,
            BigDecimal extractedTotal,
            BigDecimal extractedAmountPaid,
            BigDecimal computedTotal,
            ReceiptFieldConfidence subtotalConfidence,
            ReceiptFieldConfidence taxConfidence,
            ReceiptFieldConfidence totalConfidence,
            ReceiptFieldConfidence amountPaidConfidence,
            BigDecimal summaryRegionSubtotal,
            BigDecimal summaryRegionTax,
            BigDecimal summaryRegionTotal,
            BigDecimal summaryRegionAmountPaid,
            List<BigDecimal> amountCandidates,
            ReceiptAmountConfidence amountConfidence,
            JobCostCategory suggestedCategory,
            String notes,
            Integer confidence,
            String rawExtractedText,
            String summaryRegionRawText,
            List<String> extractionWarnings,
            /** Parsed from numeric summary OCR; supporting evidence only. */
            BigDecimal extractedTaxRatePercent
    ) {
    }
}
