package com.roofingcrm.service.accounting;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReceiptSummaryTextParserTest {

    private final ReceiptSummaryTextParser parser = new ReceiptSummaryTextParser();

    @Test
    void toExtractedReceiptData_prefersLabeledSummaryLines() {
        String summary = """
                SUBTOTAL $1,455.24
                TAX 109.14
                TOTAL 1564.38
                AMOUNT PAID 1564.38
                """;
        ReceiptExtractionClient.ExtractedReceiptData data = parser.toExtractedReceiptData(summary, "");
        assertEquals(new BigDecimal("1455.24"), data.subtotal());
        assertEquals(new BigDecimal("109.14"), data.tax());
        assertEquals(new BigDecimal("1564.38"), data.total());
        assertEquals(new BigDecimal("1564.38"), data.amountPaid());
    }

    @Test
    void parseLabeledAmounts_derivesTotalFromSubtotalAndTaxWhenTotalMissing() {
        String summary = """
                SUBTOTAL 10.00
                TAX 1.50
                """;
        ReceiptSummaryTextParser.ParsedSummary parsed = parser.parseLabeledAmounts(summary);
        assertEquals(new BigDecimal("10.00"), parsed.subtotal());
        assertEquals(new BigDecimal("1.50"), parsed.tax());
        assertEquals(new BigDecimal("11.50"), parsed.total());
    }

    @Test
    void toExtractedReceiptData_returnsEmptyWhenNoText() {
        ReceiptExtractionClient.ExtractedReceiptData data = parser.toExtractedReceiptData("", "");
        assertNull(data.subtotal());
        assertNull(data.rawExtractedText());
    }
}
