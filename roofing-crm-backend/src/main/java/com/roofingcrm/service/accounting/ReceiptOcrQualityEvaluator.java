package com.roofingcrm.service.accounting;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classifies OCR text quality so low-quality OCR is not used as numeric evidence.
 */
@Component
public class ReceiptOcrQualityEvaluator {

    private static final Pattern MONEY_PATTERN =
            Pattern.compile("(?:\\$\\s*)?(\\d{1,3}(?:,\\d{3})*|\\d+)\\.\\d{2}(?!\\d)");

    private static final int MIN_SUMMARY_CHARS_FOR_NON_LOW = 12;
    private static final int MIN_FULL_CHARS_FOR_MEDIUM = 30;
    private static final int MIN_FULL_CHARS_FOR_HIGH = 60;

    /**
     * Full-page / body OCR: used for context and interpretation hints, not primary totals.
     */
    public ReceiptOcrQuality evaluateFullPage(String text) {
        String t = trimToNull(text);
        if (t == null) {
            return ReceiptOcrQuality.LOW;
        }
        double noise = noiseRatio(t);
        int len = t.length();
        if (noise > 0.55 || len < 12) {
            return ReceiptOcrQuality.LOW;
        }
        if (len >= MIN_FULL_CHARS_FOR_HIGH && noise < 0.38) {
            return ReceiptOcrQuality.HIGH;
        }
        if (len >= MIN_FULL_CHARS_FOR_MEDIUM && noise < 0.48) {
            return ReceiptOcrQuality.MEDIUM;
        }
        return ReceiptOcrQuality.LOW;
    }

    /**
     * Summary-region OCR: must look like a labeled totals block with plausible money tokens.
     */
    public ReceiptOcrQuality evaluateSummary(String text) {
        String t = trimToNull(text);
        if (t == null || t.length() < MIN_SUMMARY_CHARS_FOR_NON_LOW) {
            return ReceiptOcrQuality.LOW;
        }
        String upper = t.toUpperCase(Locale.ROOT);
        boolean hasLabel = containsAny(upper,
                "SUBTOTAL", "TOTAL", "TAX", "GST", "VAT", "PAID", "PAYMENT", "DUE", "BALANCE", "AMOUNT");
        int moneyCount = countMoneyMatches(t);
        double noise = noiseRatio(t);

        if (!hasLabel || moneyCount == 0) {
            return ReceiptOcrQuality.LOW;
        }
        if (noise > 0.52) {
            return ReceiptOcrQuality.LOW;
        }
        if (moneyCount >= 2 && noise < 0.35 && t.length() >= 20) {
            return ReceiptOcrQuality.HIGH;
        }
        if (moneyCount >= 1 && noise < 0.45 && t.length() >= 16) {
            return ReceiptOcrQuality.MEDIUM;
        }
        return ReceiptOcrQuality.LOW;
    }

    private static int countMoneyMatches(String text) {
        int n = 0;
        Matcher m = MONEY_PATTERN.matcher(text);
        while (m.find()) {
            n++;
        }
        return n;
    }

    /**
     * Share of characters that look like OCR junk (symbols not typical on receipts).
     */
    static double noiseRatio(String s) {
        if (s.isEmpty()) {
            return 1d;
        }
        int bad = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                continue;
            }
            if (Character.isWhitespace(c)) {
                continue;
            }
            if ("$.,%:-/()'\"#&*@[]{}|\\".indexOf(c) >= 0) {
                continue;
            }
            bad++;
        }
        return (double) bad / s.length();
    }

    private static String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean containsAny(String upper, String... needles) {
        for (String n : needles) {
            if (upper.contains(n)) {
                return true;
            }
        }
        return false;
    }
}
