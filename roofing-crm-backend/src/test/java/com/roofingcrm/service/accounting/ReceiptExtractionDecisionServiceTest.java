package com.roofingcrm.service.accounting;

import com.roofingcrm.domain.enums.JobCostCategory;
import com.roofingcrm.domain.enums.ReceiptAmountConfidence;
import com.roofingcrm.domain.enums.ReceiptFieldConfidence;
import com.roofingcrm.domain.enums.ReceiptTotalSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptExtractionDecisionServiceTest {

    private final ReceiptExtractionDecisionService service = new ReceiptExtractionDecisionService();

    @Test
    void decideAmount_prefersMathValidatedSummaryTotal() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(new ReceiptAmountCandidateExtractor.RankedAmountCandidate(
                                new BigDecimal("1564.38"), 80, "GRAND TOTAL 1564.38", 10)),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(null, null, new BigDecimal("1564.38"), null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", null, null, new BigDecimal("1564.38"), null, new BigDecimal("1564.38"),
                        JobCostCategory.MATERIAL, "Receipt", 90, "GRAND TOTAL 1564.38"),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("109.14"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("1564.38"), ReceiptFieldConfidence.MEDIUM,
                        null, ReceiptFieldConfidence.UNKNOWN)
        );

        assertEquals(new BigDecimal("1564.38"), decision.amount());
        assertEquals(new BigDecimal("1564.38"), decision.computedTotal());
        assertEquals(ReceiptAmountConfidence.HIGH, decision.amountConfidence());
        assertEquals(ReceiptFieldConfidence.HIGH, decision.totalConfidence());
        assertEquals(new BigDecimal("7.5"), decision.taxRatePercent());
        assertEquals(ReceiptFieldConfidence.HIGH, decision.taxRateConfidence());
    }

    @Test
    void decideAmount_computedTotalCorrectsSlightlyWrongExtractedTotal() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(
                                new ReceiptAmountCandidateExtractor.RankedAmountCandidate(
                                        new BigDecimal("1564.38"), 55, "BALANCE DUE 1564.38", 10),
                                new ReceiptAmountCandidateExtractor.RankedAmountCandidate(
                                        new BigDecimal("1563.65"), 50, "TOTAL 1563.65", 9)
                        ),
                        List.of("Multiple possible totals detected. Please confirm before saving."),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(
                                new BigDecimal("1455.24"), new BigDecimal("109.14"), new BigDecimal("1563.65"), null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", new BigDecimal("1455.24"), new BigDecimal("109.14"),
                        new BigDecimal("1563.65"), null, new BigDecimal("1563.65"),
                        JobCostCategory.MATERIAL, "Receipt", 75, "..."),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("109.14"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("1563.65"), ReceiptFieldConfidence.LOW,
                        new BigDecimal("1563.65"), ReceiptFieldConfidence.LOW)
        );

        assertEquals(new BigDecimal("1564.38"), decision.amount());
        assertEquals(new BigDecimal("1564.38"), decision.computedTotal());
        assertEquals(ReceiptFieldConfidence.HIGH, decision.totalConfidence());
        assertTrue(decision.amountCandidates().contains(new BigDecimal("1563.65")));
        assertEquals(ReceiptAmountConfidence.HIGH, decision.amountConfidence());
    }

    @Test
    void decideAmount_amountPaidDoesNotOverrideMathValidatedTotal() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(
                                new ReceiptAmountCandidateExtractor.RankedAmountCandidate(
                                        new BigDecimal("1563.65"), 90, "AMOUNT PAID 1563.65", 10),
                                new ReceiptAmountCandidateExtractor.RankedAmountCandidate(
                                        new BigDecimal("1564.38"), 40, "GRAND TOTAL 1564.38", 11)
                        ),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(
                                new BigDecimal("1455.24"), new BigDecimal("109.14"), null, null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", new BigDecimal("1455.24"), new BigDecimal("109.14"),
                        new BigDecimal("1564.38"), new BigDecimal("1563.65"), new BigDecimal("1563.65"),
                        JobCostCategory.MATERIAL, "Receipt", 75, "..."),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("109.14"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("1564.38"), ReceiptFieldConfidence.MEDIUM,
                        new BigDecimal("1563.65"), ReceiptFieldConfidence.MEDIUM)
        );

        assertEquals(new BigDecimal("1564.38"), decision.amount());
        assertEquals(ReceiptAmountConfidence.HIGH, decision.amountConfidence());
        assertTrue(decision.warnings().stream().anyMatch(warning -> warning.contains("amount paid")));
    }

    @Test
    void decideAmount_computedTotalBecomesCandidateWhenDifferentFromExtractedTotal() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(
                                new BigDecimal("1455.24"), new BigDecimal("109.14"), null, null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", new BigDecimal("1455.24"), new BigDecimal("109.14"),
                        new BigDecimal("1563.65"), null, null,
                        JobCostCategory.MATERIAL, "Receipt", 70, "..."),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("109.14"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("1563.65"), ReceiptFieldConfidence.LOW,
                        null, ReceiptFieldConfidence.UNKNOWN)
        );

        assertEquals(new BigDecimal("1564.38"), decision.amount());
        assertTrue(decision.amountCandidates().contains(new BigDecimal("1563.65")));
        assertTrue(decision.amountCandidates().contains(new BigDecimal("1564.38")));
        assertEquals(ReceiptAmountConfidence.HIGH, decision.amountConfidence());
    }

    @Test
    void decideAmount_reliableSubtotalAndTotalCanCorrectWeakTax() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(
                                new BigDecimal("1455.24"), new BigDecimal("108.41"), new BigDecimal("1564.38"), null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", new BigDecimal("1455.24"), new BigDecimal("108.41"),
                        new BigDecimal("1564.38"), null, null, JobCostCategory.MATERIAL, "Receipt", 80, "..."),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("108.41"), ReceiptFieldConfidence.LOW,
                        new BigDecimal("1564.38"), ReceiptFieldConfidence.HIGH,
                        null, ReceiptFieldConfidence.UNKNOWN)
        );

        assertEquals(new BigDecimal("109.14"), decision.tax());
        assertEquals(ReceiptFieldConfidence.HIGH, decision.taxConfidence());
        assertEquals(new BigDecimal("1564.38"), decision.amount());
        assertTrue(decision.warnings().stream().anyMatch(warning -> warning.contains("Tax total was derived")));
        assertTrue(decision.taxDerived());
        assertEquals(new BigDecimal("7.5"), decision.taxRatePercent());
    }

    @Test
    void decideAmount_derivesMissingTaxFromTrustedSubtotalAndTotal() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(
                                new BigDecimal("1455.24"), null, new BigDecimal("1564.38"), null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", new BigDecimal("1455.24"), null,
                        new BigDecimal("1564.38"), null, null, JobCostCategory.MATERIAL, "Receipt", 80, "..."),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.HIGH,
                        null, ReceiptFieldConfidence.UNKNOWN,
                        new BigDecimal("1564.38"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("1564.38"), ReceiptFieldConfidence.HIGH)
        );

        assertEquals(new BigDecimal("109.14"), decision.tax());
        assertEquals(ReceiptFieldConfidence.HIGH, decision.taxConfidence());
        assertTrue(decision.warnings().stream().anyMatch(warning -> warning.contains("Tax total was derived")));
        assertTrue(decision.taxDerived());
    }

    @Test
    void decideAmount_keepsCorrectTaxWithoutAddingCorrectionWarning() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(
                                new BigDecimal("1455.24"), new BigDecimal("109.14"), new BigDecimal("1564.38"), null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", new BigDecimal("1455.24"), new BigDecimal("109.14"),
                        new BigDecimal("1564.38"), null, null, JobCostCategory.MATERIAL, "Receipt", 80, "..."),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("109.14"), ReceiptFieldConfidence.MEDIUM,
                        new BigDecimal("1564.38"), ReceiptFieldConfidence.HIGH,
                        null, ReceiptFieldConfidence.UNKNOWN)
        );

        assertEquals(new BigDecimal("109.14"), decision.tax());
        assertTrue(decision.warnings().stream().noneMatch(warning -> warning.contains("Tax total was derived from subtotal and total")));
        assertTrue(decision.warnings().stream().noneMatch(warning -> warning.contains("Tax rate was derived")));
        assertFalse(decision.taxDerived());
        assertTrue(decision.warnings().stream().noneMatch(w -> w.contains("Tax total was derived from subtotal and tax rate")));
        assertTrue(decision.warnings().stream().noneMatch(w -> w.contains("Total was derived from subtotal and tax.")));
        assertTrue(decision.warnings().stream().noneMatch(w -> w.contains("Amount paid was aligned")));
    }

    @Test
    void decideAmount_finalReconciliation_fixesInternallyConsistentWrongTaxTotalAndAlignsPaid() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(
                                new BigDecimal("1455.24"), new BigDecimal("108.41"),
                                new BigDecimal("1563.65"), new BigDecimal("1563.65"))
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", new BigDecimal("1455.24"), new BigDecimal("108.41"),
                        new BigDecimal("1563.65"), new BigDecimal("1563.65"), new BigDecimal("1563.65"),
                        JobCostCategory.MATERIAL, "Receipt", 80, "..."),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("108.41"), ReceiptFieldConfidence.MEDIUM,
                        new BigDecimal("1563.65"), ReceiptFieldConfidence.MEDIUM,
                        new BigDecimal("1563.65"), ReceiptFieldConfidence.MEDIUM)
        );

        assertEquals(new BigDecimal("109.14"), decision.tax());
        assertEquals(new BigDecimal("1564.38"), decision.total());
        assertEquals(new BigDecimal("1564.38"), decision.amountPaid());
        assertEquals(new BigDecimal("1564.38"), decision.amount());
        assertEquals(new BigDecimal("1564.38"), decision.computedTotal());
        assertEquals(new BigDecimal("7.5"), decision.taxRatePercent());
        assertTrue(decision.warnings().stream().anyMatch(w -> w.contains("Tax total was derived from subtotal and tax rate")));
        assertTrue(decision.warnings().stream().anyMatch(w -> w.contains("Total was derived from subtotal and tax")));
        assertTrue(decision.warnings().stream().anyMatch(w -> w.contains("Amount paid was aligned")));
    }

    @Test
    void decideAmount_doesNotReconcileFromSubtotalTaxRate_whenTaxConfidenceTooLowForRate() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(
                                new BigDecimal("1455.24"), new BigDecimal("108.41"),
                                new BigDecimal("1563.65"), new BigDecimal("1563.65"))
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", new BigDecimal("1455.24"), new BigDecimal("108.41"),
                        new BigDecimal("1563.65"), new BigDecimal("1563.65"), new BigDecimal("1563.65"),
                        JobCostCategory.MATERIAL, "Receipt", 80, "..."),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("108.41"), ReceiptFieldConfidence.LOW,
                        new BigDecimal("1563.65"), ReceiptFieldConfidence.MEDIUM,
                        new BigDecimal("1563.65"), ReceiptFieldConfidence.MEDIUM)
        );

        assertEquals(new BigDecimal("108.41"), decision.tax());
        assertEquals(new BigDecimal("1563.65"), decision.total());
        assertTrue(decision.warnings().stream().noneMatch(w -> w.contains("Tax total was derived from subtotal and tax rate")));
    }

    @Test
    void decideAmount_doesNotOvercorrectTaxWhenSubtotalOrTotalAreWeak() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(null, null, null, null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", null, null, null, null, null,
                        JobCostCategory.MATERIAL, "Receipt", 80, "..."),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.LOW,
                        new BigDecimal("108.41"), ReceiptFieldConfidence.LOW,
                        new BigDecimal("1564.38"), ReceiptFieldConfidence.MEDIUM,
                        null, ReceiptFieldConfidence.UNKNOWN)
        );

        assertEquals(new BigDecimal("108.41"), decision.tax());
        assertEquals(ReceiptFieldConfidence.LOW, decision.taxConfidence());
        assertTrue(decision.warnings().stream().noneMatch(w -> w.contains("Tax total was derived from subtotal and tax rate")));
        assertTrue(decision.warnings().stream().noneMatch(w -> w.contains("Tax total was derived from subtotal and total")));
    }

    @Test
    void decideAmount_correctsWrongTaxWhenExtractedTaxIsMediumButInconsistentWithSubtotalAndTotal() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(
                                new BigDecimal("1455.24"), new BigDecimal("108.41"), new BigDecimal("1564.38"), null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", new BigDecimal("1455.24"), new BigDecimal("108.41"),
                        new BigDecimal("1564.38"), null, null, JobCostCategory.MATERIAL, "Receipt", 80, "..."),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("108.41"), ReceiptFieldConfidence.LOW,
                        new BigDecimal("1564.38"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("1564.38"), ReceiptFieldConfidence.HIGH)
        );

        assertEquals(new BigDecimal("109.14"), decision.tax());
        assertTrue(decision.taxDerived());
        assertEquals(new BigDecimal("7.5"), decision.taxRatePercent());
    }

    @Test
    void decideAmount_doesNotDeriveTaxFromAmountPaidAloneWhenTotalMissing() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(
                                new BigDecimal("1455.24"), new BigDecimal("108.41"), null, new BigDecimal("1563.65"))
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", new BigDecimal("1455.24"), new BigDecimal("108.41"),
                        null, new BigDecimal("1563.65"), null,
                        JobCostCategory.MATERIAL, "Receipt", 80, "..."),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("108.41"), ReceiptFieldConfidence.LOW,
                        null, ReceiptFieldConfidence.UNKNOWN,
                        new BigDecimal("1563.65"), ReceiptFieldConfidence.MEDIUM)
        );

        assertEquals(new BigDecimal("108.41"), decision.tax());
        assertEquals(new BigDecimal("1563.65"), decision.amountPaid());
        assertNull(decision.amount());
        assertTrue(decision.warnings().stream().noneMatch(warning -> warning.contains("Tax total was derived")));
    }

    @Test
    void decideAmount_addsWarningWhenSummaryMathConflicts() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(
                                new BigDecimal("1455.24"), new BigDecimal("109.14"), new BigDecimal("1545.38"), null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", new BigDecimal("1455.24"), new BigDecimal("109.14"),
                        new BigDecimal("1545.38"), null, null,
                        JobCostCategory.MATERIAL, "Receipt", 55, "..."),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("109.14"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("1564.38"), ReceiptFieldConfidence.MEDIUM,
                        null, ReceiptFieldConfidence.UNKNOWN)
        );

        assertEquals(new BigDecimal("1564.38"), decision.amount());
        assertTrue(decision.warnings().stream().anyMatch(warning -> warning.contains("Summary region total does not match")
                || warning.contains("summary region")));
    }

    @Test
    void decideAmount_amountPaidCanSupportButNotReplaceTrustedCandidateTotal() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(new ReceiptAmountCandidateExtractor.RankedAmountCandidate(
                                new BigDecimal("1564.38"), 72, "GRAND TOTAL 1564.38", 10)),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(
                                new BigDecimal("1455.24"), null, null, new BigDecimal("1564.38"))
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", new BigDecimal("1455.24"), null,
                        null, new BigDecimal("1564.38"), null,
                        JobCostCategory.MATERIAL, "Receipt", 80, "..."),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.HIGH,
                        null, ReceiptFieldConfidence.UNKNOWN,
                        null, ReceiptFieldConfidence.UNKNOWN,
                        new BigDecimal("1564.38"), ReceiptFieldConfidence.MEDIUM)
        );

        assertEquals(new BigDecimal("1564.38"), decision.amount());
        assertEquals(ReceiptAmountConfidence.MEDIUM, decision.amountConfidence());
        assertEquals(ReceiptTotalSource.VISION_AMOUNT_PAID, decision.totalSource());
        assertEquals(new BigDecimal("109.14"), decision.tax());
        assertTrue(decision.taxDerived());
    }

    @Test
    void decideAmount_fullImageTotalUsedOnlyWhenConsistentWithSummaryEvidence() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(
                                new ReceiptAmountCandidateExtractor.RankedAmountCandidate(
                                        new BigDecimal("1654.79"), 95, "TOTAL 1654.79", 10),
                                new ReceiptAmountCandidateExtractor.RankedAmountCandidate(
                                        new BigDecimal("1564.38"), 70, "GRAND TOTAL 1564.38", 11)
                        ),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(
                                new BigDecimal("1455.24"), new BigDecimal("109.14"), new BigDecimal("1654.79"), null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", new BigDecimal("1455.24"), new BigDecimal("109.14"),
                        new BigDecimal("1654.79"), null, new BigDecimal("1654.79"),
                        JobCostCategory.MATERIAL, "Receipt", 62, "..."),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("109.14"), ReceiptFieldConfidence.HIGH,
                        null, ReceiptFieldConfidence.UNKNOWN,
                        null, ReceiptFieldConfidence.UNKNOWN)
        );

        assertEquals(new BigDecimal("1564.38"), decision.amount());
        assertEquals(ReceiptAmountConfidence.HIGH, decision.amountConfidence());
        assertEquals(ReceiptTotalSource.COMPUTED_SUBTOTAL_PLUS_TAX, decision.totalSource());
    }

    /**
     * Regression: full-image vision returns a wrong TOTAL line (e.g. 1654.79) while summary crops agree on
     * subtotal+tax (1564.38 computed). The stable total must come from summary consensus, not regex line scores.
     */
    @Test
    void decideAmount_regression_wrongFullImageTotalDoesNotOverrideTrustedSummarySubtotalPlusTax() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(new ReceiptAmountCandidateExtractor.RankedAmountCandidate(
                                new BigDecimal("1654.79"), 99, "WRONG TOTAL 1654.79", 0)),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(null, null, null, null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28",
                        new BigDecimal("1455.24"), new BigDecimal("109.14"),
                        new BigDecimal("1654.79"), null, new BigDecimal("1654.79"),
                        JobCostCategory.MATERIAL, "Receipt", 72, "full vision raw"),
                summary(new BigDecimal("1455.24"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("109.14"), ReceiptFieldConfidence.HIGH,
                        null, ReceiptFieldConfidence.UNKNOWN,
                        null, ReceiptFieldConfidence.UNKNOWN)
        );

        assertEquals(new BigDecimal("1564.38"), decision.amount());
        assertEquals(ReceiptTotalSource.COMPUTED_SUBTOTAL_PLUS_TAX, decision.totalSource());
    }

    /**
     * Regression: per-field consensus (or a manually wrong consensus) would pick a wrong total; one summary crop is
     * internally math-coherent with the correct total. Final amount must come from that attempt, not consensus or full-image.
     */
    @Test
    void decideAmount_prefersBestCoherentSummaryAttemptOverWrongConsensusAndFullImage() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(new ReceiptAmountCandidateExtractor.RankedAmountCandidate(
                                new BigDecimal("1654.79"), 99, "WRONG LINE 1654.79", 0)),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(null, null, null, null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28",
                        new BigDecimal("1455.24"), new BigDecimal("109.14"),
                        new BigDecimal("1654.79"), null, new BigDecimal("1654.79"),
                        JobCostCategory.MATERIAL, "Receipt", 72, "full vision raw"),
                summary(
                        new BigDecimal("1654.79"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("109.14"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("1654.79"), ReceiptFieldConfidence.HIGH,
                        new BigDecimal("1654.79"), ReceiptFieldConfidence.HIGH),
                false,
                List.of(
                        summaryAttempt("tight", 3, "baseline", 3,
                                new BigDecimal("1455.24"), new BigDecimal("109.14"),
                                new BigDecimal("1654.79"), new BigDecimal("1654.79"),
                                85, "noisy wrong total"),
                        summaryAttempt("large", 2, "threshold", 2,
                                new BigDecimal("1455.24"), new BigDecimal("109.14"),
                                new BigDecimal("1564.38"), new BigDecimal("1564.38"),
                                88, "coherent correct total")
                )
        );

        assertEquals(new BigDecimal("1564.38"), decision.amount());
        assertEquals(ReceiptTotalSource.BEST_SUMMARY_COHERENT_ATTEMPT, decision.totalSource());
    }

    @Test
    void decideAmount_trustedVisionTotalWinsBeforeNoisyLineCandidates() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(new ReceiptAmountCandidateExtractor.RankedAmountCandidate(
                                new BigDecimal("199.99"), 95, "NOISY LINE 199.99", 1)),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(null, null, null, null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "Store", "2026-03-28", null, null,
                        new BigDecimal("200.00"), null, null,
                        JobCostCategory.MATERIAL, "Receipt", 88, "TOTAL 200.00"),
                summary(null, ReceiptFieldConfidence.UNKNOWN,
                        null, ReceiptFieldConfidence.UNKNOWN,
                        null, ReceiptFieldConfidence.UNKNOWN,
                        null, ReceiptFieldConfidence.UNKNOWN)
        );

        assertEquals(new BigDecimal("200.00"), decision.amount());
        assertEquals(new BigDecimal("200.00"), decision.total());
        assertEquals(ReceiptTotalSource.VISION_TOTAL, decision.totalSource());
    }

    @Test
    void decideAmount_visionAmountPaidUsedWhenNoCandidatesAndNoSummaryComputedTotal() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(null, null, null, null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "Store", "2026-03-28", null, null,
                        null, new BigDecimal("42.50"), null,
                        JobCostCategory.MATERIAL, "Receipt", 85, "PAID 42.50"),
                summary(null, ReceiptFieldConfidence.UNKNOWN,
                        null, ReceiptFieldConfidence.UNKNOWN,
                        null, ReceiptFieldConfidence.UNKNOWN,
                        null, ReceiptFieldConfidence.UNKNOWN)
        );

        assertEquals(new BigDecimal("42.50"), decision.amount());
        assertEquals(new BigDecimal("42.50"), decision.total());
        assertEquals(ReceiptTotalSource.VISION_AMOUNT_PAID, decision.totalSource());
    }

    @Test
    void decideAmount_lineCandidatesNotUsedForFinalTotalAddsWarningWhenPresent() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(new ReceiptAmountCandidateExtractor.RankedAmountCandidate(
                                new BigDecimal("99.99"), 90, "TOTAL 99.99", 1)),
                        List.of(),
                        new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(null, null, null, null)
                ),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "Store", "2026-03-28", null, null, null, null, null,
                        JobCostCategory.MATERIAL, "Receipt", 40, "no totals"),
                summary(null, ReceiptFieldConfidence.UNKNOWN,
                        null, ReceiptFieldConfidence.UNKNOWN,
                        null, ReceiptFieldConfidence.UNKNOWN,
                        null, ReceiptFieldConfidence.UNKNOWN)
        );

        assertNull(decision.amount());
        assertEquals(ReceiptTotalSource.NONE, decision.totalSource());
        assertTrue(decision.warnings().stream().anyMatch(w -> w.contains("Line-based amount candidates were not used")));
    }

    @Test
    void decideAmount_returnsNullWhenNoReliableTotalExists() {
        var decision = service.decideAmount(
                new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                        List.of(), List.of(), new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(null, null, null, null)),
                new ReceiptExtractionClient.ExtractedReceiptData(
                        "ABC Supply", "2026-03-28", null, null, null, null, null,
                        JobCostCategory.MATERIAL, "Receipt", 40, "No total visible"),
                summary(null, ReceiptFieldConfidence.UNKNOWN,
                        null, ReceiptFieldConfidence.UNKNOWN,
                        null, ReceiptFieldConfidence.UNKNOWN,
                        null, ReceiptFieldConfidence.UNKNOWN)
        );

        assertNull(decision.amount());
        assertEquals(ReceiptAmountConfidence.LOW, decision.amountConfidence());
    }

    private SummaryFieldConsensusService.SummaryConsensusResult summary(
            BigDecimal subtotal,
            ReceiptFieldConfidence subtotalConfidence,
            BigDecimal tax,
            ReceiptFieldConfidence taxConfidence,
            BigDecimal total,
            ReceiptFieldConfidence totalConfidence,
            BigDecimal amountPaid,
            ReceiptFieldConfidence amountPaidConfidence) {
        return new SummaryFieldConsensusService.SummaryConsensusResult(
                subtotal,
                subtotalConfidence,
                tax,
                taxConfidence,
                total,
                totalConfidence,
                amountPaid,
                amountPaidConfidence,
                "summary text",
                List.of()
        );
    }

    private SummaryFieldConsensusService.SummaryExtractionAttempt summaryAttempt(
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
