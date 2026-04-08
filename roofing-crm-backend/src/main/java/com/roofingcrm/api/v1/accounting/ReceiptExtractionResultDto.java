package com.roofingcrm.api.v1.accounting;

import com.roofingcrm.domain.enums.JobCostCategory;
import com.roofingcrm.domain.enums.ReceiptAmountConfidence;
import com.roofingcrm.domain.enums.ReceiptFieldConfidence;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class ReceiptExtractionResultDto {

    private String vendorName;
    private Instant incurredAt;
    private BigDecimal amount;
    private BigDecimal extractedSubtotal;
    private BigDecimal extractedTax;
    private BigDecimal extractedTotal;
    private BigDecimal extractedAmountPaid;
    private BigDecimal computedTotal;
    private ReceiptFieldConfidence subtotalConfidence;
    private ReceiptFieldConfidence taxConfidence;
    private ReceiptFieldConfidence totalConfidence;
    private ReceiptFieldConfidence amountPaidConfidence;
    private BigDecimal summaryRegionSubtotal;
    private BigDecimal summaryRegionTax;
    private BigDecimal summaryRegionTotal;
    private BigDecimal summaryRegionAmountPaid;
    private List<BigDecimal> amountCandidates;
    private ReceiptAmountConfidence amountConfidence;
    private JobCostCategory suggestedCategory;
    private String notes;
    private Integer confidence;
    private String rawExtractedText;
    private String summaryRegionRawText;
    private List<String> extractionWarnings;
    /** Parsed from numeric summary OCR; supporting evidence only. */
    private BigDecimal extractedTaxRatePercent;
}
