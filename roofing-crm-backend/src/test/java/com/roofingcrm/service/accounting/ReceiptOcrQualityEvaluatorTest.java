package com.roofingcrm.service.accounting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReceiptOcrQualityEvaluatorTest {

    private final ReceiptOcrQualityEvaluator evaluator = new ReceiptOcrQualityEvaluator();

    @Test
    void summary_gibberishIsLow() {
        assertEquals(ReceiptOcrQuality.LOW,
                evaluator.evaluateSummary("@@@###$$$%%%"));
    }

    @Test
    void summary_tooShortIsLow() {
        assertEquals(ReceiptOcrQuality.LOW, evaluator.evaluateSummary("TOTAL 1.00"));
    }

    @Test
    void summary_labeledWithMoneyIsHighOrMedium() {
        String good = """
                SUBTOTAL 1455.24
                TAX 109.14
                TOTAL 1564.38
                AMOUNT PAID 1564.38
                """.trim();
        ReceiptOcrQuality q = evaluator.evaluateSummary(good);
        assertEquals(ReceiptOcrQuality.HIGH, q);
    }

    @Test
    void summary_subtotalLabelOnlyNoMoneyIsLow() {
        assertEquals(ReceiptOcrQuality.LOW,
                evaluator.evaluateSummary("Subtotal and tax may apply to your order"));
    }

    @Test
    void fullPage_cleanLongTextIsHigh() {
        String body = "STORE NAME\n" + "LINE ".repeat(30) + "\nTOTAL 99.99\n";
        assertEquals(ReceiptOcrQuality.HIGH, evaluator.evaluateFullPage(body));
    }

    @Test
    void fullPage_shortIsLow() {
        assertEquals(ReceiptOcrQuality.LOW, evaluator.evaluateFullPage("short"));
    }

    @Test
    void noiseRatio_allLettersIsZero() {
        assertEquals(0d, ReceiptOcrQualityEvaluator.noiseRatio("abcXYZ"), 0.001);
    }
}
