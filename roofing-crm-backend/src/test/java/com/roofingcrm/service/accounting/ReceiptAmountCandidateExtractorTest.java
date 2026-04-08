package com.roofingcrm.service.accounting;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptAmountCandidateExtractorTest {

    private final ReceiptAmountCandidateExtractor extractor = new ReceiptAmountCandidateExtractor();

    @Test
    void extractCandidates_prefersSingleTotalLine() {
        var result = extractor.extractCandidates("""
                ABC Supply
                Subtotal 120.00
                Tax 9.60
                TOTAL 129.60
                """);

        assertEquals(new BigDecimal("129.60"), result.candidates().getFirst().amount());
    }

    @Test
    void extractCandidates_prefersTotalOverSubtotal() {
        var result = extractor.extractCandidates("""
                RECEIPT
                SUBTOTAL $1,545.38
                TAX $19.00
                GRAND TOTAL $1,564.38
                """);

        assertEquals(new BigDecimal("1564.38"), result.candidates().getFirst().amount());
    }

    @Test
    void extractCandidates_boostsSubtotalPlusTaxMatch() {
        var result = extractor.extractCandidates("""
                Materials
                SUBTOTAL 1500.00
                SALES TAX 64.38
                TOTAL DUE 1564.38
                """);

        assertEquals(new BigDecimal("1564.38"), result.candidates().getFirst().amount());
    }

    @Test
    void extractCandidates_prefersLowerReceiptTotalsWhenAmbiguous() {
        var result = extractor.extractCandidates("""
                TOTAL 120.00
                Item price 50.00
                Tax 4.00
                GRAND TOTAL 124.00
                """);

        assertEquals(new BigDecimal("124.00"), result.candidates().getFirst().amount());
    }

    @Test
    void extractCandidates_prefersSummaryLabelsOverBodyAmounts() {
        var result = extractor.extractCandidates("""
                ITEM A 1455.24
                RATE 1545.38
                SUBTOTAL 1455.24
                TAX 109.14
                TOTAL 1564.38
                """);

        assertEquals(new BigDecimal("1564.38"), result.candidates().getFirst().amount());
        assertEquals(new BigDecimal("1455.24"), result.summaryAmounts().subtotal());
        assertEquals(new BigDecimal("109.14"), result.summaryAmounts().tax());
        assertEquals(new BigDecimal("1564.38"), result.summaryAmounts().total());
    }

    @Test
    void extractCandidates_deprioritizesWrongLineItemNearTotal() {
        var result = extractor.extractCandidates("""
                MATERIAL RATE 1545.38
                DESCRIPTION SHINGLE BUNDLE
                SUBTOTAL 1455.24
                TAX TOTAL 109.14
                GRAND TOTAL 1564.38
                """);

        assertEquals(new BigDecimal("1564.38"), result.candidates().getFirst().amount());
    }

    @Test
    void extractCandidates_warnsWhenMultiplePlausibleTotalsExist() {
        var result = extractor.extractCandidates("""
                TOTAL 1545.38
                BALANCE DUE 1564.38
                """);

        assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("Multiple possible totals")));
    }
}
