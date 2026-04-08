package com.roofingcrm.service.accounting;

import com.roofingcrm.domain.enums.ReceiptFieldConfidence;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryFieldConsensusServiceTest {

    private final SummaryFieldConsensusService service = new SummaryFieldConsensusService();

    @Test
    void buildConsensus_prefersSupportedValidatedValuesAcrossVariants() {
        var consensus = service.buildConsensus(List.of(
                attempt("tight", 3, "baseline", 3, new BigDecimal("1455.24"), new BigDecimal("109.14"), new BigDecimal("1564.38"), new BigDecimal("1564.38"), 88, "SUBTOTAL 1455.24 TAX 109.14 TOTAL 1564.38"),
                attempt("large", 2, "threshold", 2, new BigDecimal("1455.24"), new BigDecimal("109.14"), new BigDecimal("1564.38"), null, 84, "SUBTOTAL 1455.24 TAX 109.14 GRAND TOTAL 1564.38"),
                attempt("expanded", 2, "sharpened", 2, new BigDecimal("1455.24"), new BigDecimal("108.41"), new BigDecimal("1563.65"), new BigDecimal("1563.65"), 70, "SUBTOTAL 1455.24 TAX 108.41 TOTAL 1563.65")
        ));

        assertEquals(new BigDecimal("1455.24"), consensus.subtotal());
        assertEquals(ReceiptFieldConfidence.HIGH, consensus.subtotalConfidence());
        assertEquals(new BigDecimal("109.14"), consensus.tax());
        assertEquals(ReceiptFieldConfidence.HIGH, consensus.taxConfidence());
        assertEquals(new BigDecimal("1564.38"), consensus.total());
        assertEquals(ReceiptFieldConfidence.HIGH, consensus.totalConfidence());
        assertEquals(new BigDecimal("1564.38"), consensus.amountPaid());
    }

    /**
     * Regression: two noisy summary attempts report a wrong TOTAL that does not match subtotal+tax (votes cluster
     * on the wrong number); one attempt is internally coherent (correct total). Consensus must pick the coherent
     * attempt for the summary fields, not the diluted wrong total.
     */
    @Test
    void buildConsensus_prefersInternallyCoherentAttemptWhenNoisyTotalsOutvoteCorrectTotal() {
        var consensus = service.buildConsensus(List.of(
                attempt("tight", 3, "baseline", 3,
                        new BigDecimal("1455.24"), new BigDecimal("109.14"),
                        new BigDecimal("1654.79"), new BigDecimal("1654.79"),
                        85, "SUBTOTAL 1455.24 TAX 109.14 TOTAL 1654.79"),
                attempt("large", 2, "threshold", 2,
                        new BigDecimal("1455.24"), new BigDecimal("109.14"),
                        new BigDecimal("1654.79"), null,
                        82, "SUBTOTAL 1455.24 TAX 109.14 TOTAL 1654.79"),
                attempt("expanded", 2, "sharpened", 2,
                        new BigDecimal("1455.24"), new BigDecimal("109.14"),
                        new BigDecimal("1564.38"), new BigDecimal("1564.38"),
                        78, "SUBTOTAL 1455.24 TAX 109.14 TOTAL 1564.38 AMOUNT PAID 1564.38")
        ));

        assertEquals(new BigDecimal("1564.38"), consensus.total());
        assertEquals(new BigDecimal("1564.38"), consensus.amountPaid());
        assertTrue(consensus.notes().stream().anyMatch(n -> n.contains("coherent attempt")));
    }

    @Test
    void buildConsensus_returnsUnknownWhenFieldMissingAcrossVariants() {
        var consensus = service.buildConsensus(List.of(
                attempt("tight", 3, "baseline", 3, null, null, null, null, 60, "blurred"),
                attempt("large", 2, "threshold", 2, null, null, null, null, 55, "unreadable")
        ));

        assertEquals(null, consensus.total());
        assertEquals(ReceiptFieldConfidence.UNKNOWN, consensus.totalConfidence());
    }

    private SummaryFieldConsensusService.SummaryExtractionAttempt attempt(
            String cropId,
            int cropWeight,
            String variantId,
            int variantWeight,
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal total,
            BigDecimal amountPaid,
            Integer confidence,
            String rawText) {
        return new SummaryFieldConsensusService.SummaryExtractionAttempt(
                cropId,
                cropWeight,
                variantId,
                variantWeight,
                new ReceiptExtractionClient.ExtractedReceiptData(
                        null,
                        null,
                        subtotal,
                        tax,
                        total,
                        amountPaid,
                        null,
                        null,
                        null,
                        confidence,
                        rawText
                )
        );
    }
}
