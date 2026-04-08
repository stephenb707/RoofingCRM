package com.roofingcrm.service.accounting;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses labeled summary totals from numeric summary text (typically vision transcriptions of the totals block).
 */
@Component
public class ReceiptSummaryTextParser {

    private static final Pattern MONEY_PATTERN =
            Pattern.compile("(?:\\$\\s*)?(\\d{1,3}(?:,\\d{3})*|\\d+)\\.\\d{2}(?!\\d)");

    private static final Pattern TAX_RATE_PATTERN =
            Pattern.compile("(?:^|[^\\d])(\\d{1,2}(?:\\.\\d{1,3})?)\\s*%");

    private static final int SUMMARY_TEXT_CONFIDENCE = 92;

    public record NumericSummaryExtraction(
            ParsedNumericSummary parsed,
            ReceiptExtractionClient.ExtractedReceiptData extractedData
    ) {
    }

    /**
     * Single pass: parse numeric summary text and build consensus-oriented {@link ReceiptExtractionClient.ExtractedReceiptData}.
     */
    public NumericSummaryExtraction extractNumericSummary(String summaryNumericText) {
        String primary = trimToNull(summaryNumericText);
        if (primary == null || primary.length() < 4) {
            return new NumericSummaryExtraction(
                    new ParsedNumericSummary(null, null, null, null, null),
                    new ReceiptExtractionClient.ExtractedReceiptData(
                            null, null, null, null, null, null, null, null, null, null, null
                    ));
        }
        ParsedNumericSummary parsed = parseNumericSummary(primary);
        ReceiptExtractionClient.ExtractedReceiptData data = new ReceiptExtractionClient.ExtractedReceiptData(
                null,
                null,
                parsed.subtotal(),
                parsed.tax(),
                parsed.total(),
                parsed.amountPaid(),
                null,
                null,
                null,
                SUMMARY_TEXT_CONFIDENCE,
                primary
        );
        return new NumericSummaryExtraction(parsed, data);
    }

    /**
     * Builds structured summary fields from numeric summary text only (no full-page fallback for money).
     */
    public ReceiptExtractionClient.ExtractedReceiptData toExtractedReceiptDataFromNumericSummary(String summaryNumericText) {
        return extractNumericSummary(summaryNumericText).extractedData();
    }

    /**
     * @deprecated Use {@link #toExtractedReceiptDataFromNumericSummary(String)} for dual-path extraction.
     */
    @Deprecated
    public ReceiptExtractionClient.ExtractedReceiptData toExtractedReceiptData(String summaryText, String fullTextFallback) {
        String primary = choosePrimaryText(summaryText, fullTextFallback);
        if (primary == null || primary.isBlank()) {
            return new ReceiptExtractionClient.ExtractedReceiptData(
                    null, null, null, null, null, null, null, null, null, null, null
            );
        }
        ParsedNumericSummary parsed = parseNumericSummary(primary);
        return new ReceiptExtractionClient.ExtractedReceiptData(
                null,
                null,
                parsed.subtotal(),
                parsed.tax(),
                parsed.total(),
                parsed.amountPaid(),
                null,
                null,
                null,
                SUMMARY_TEXT_CONFIDENCE,
                primary
        );
    }

    private static String choosePrimaryText(String summaryText, String fullTextFallback) {
        String s = trimToNull(summaryText);
        if (s != null && s.length() >= 8) {
            return s;
        }
        return trimToNull(fullTextFallback);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Parses subtotal/tax/total/amount paid and optional tax rate from summary text.
     */
    public ParsedNumericSummary parseNumericSummary(String text) {
        String[] lines = text.split("\\R");
        BigDecimal subtotal = null;
        BigDecimal tax = null;
        BigDecimal total = null;
        BigDecimal amountPaid = null;
        BigDecimal taxRatePercent = null;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            String upper = normalizeForMatching(line.toUpperCase(Locale.ROOT));
            BigDecimal rate = parseTaxRatePercent(line);
            if (rate != null && taxRatePercent == null && isTaxishLine(upper)) {
                taxRatePercent = rate;
            }
            BigDecimal amount = firstMoney(line);
            if (amount == null) {
                continue;
            }
            if (isSubtotalLine(upper) && subtotal == null) {
                subtotal = amount;
                continue;
            }
            if (isTaxAmountLine(upper) && tax == null) {
                tax = amount;
                continue;
            }
            if (isAmountPaidLine(upper) && amountPaid == null) {
                amountPaid = amount;
                continue;
            }
            if (isGrandTotalLine(upper) && total == null) {
                total = amount;
            }
        }

        if (subtotal != null && tax != null && total == null) {
            total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
        }

        return new ParsedNumericSummary(subtotal, tax, total, amountPaid, taxRatePercent);
    }

    /** Collapses noisy spacing for label matching. */
    private static String normalizeForMatching(String upper) {
        return upper.replaceAll("\\s+", " ").trim();
    }

    private static BigDecimal parseTaxRatePercent(String line) {
        Matcher matcher = TAX_RATE_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        try {
            return new BigDecimal(matcher.group(1)).setScale(3, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean isTaxishLine(String upper) {
        return upper.contains("TAX") || upper.contains("GST") || upper.contains("VAT");
    }

    private static boolean isSubtotalLine(String upper) {
        return upper.contains("SUBTOTAL")
                || upper.contains("SUB TOTAL")
                || upper.contains("SUB-TOTAL")
                || (upper.contains("SUB") && upper.contains("TOT"));
    }

    private static boolean isTaxAmountLine(String upper) {
        if (upper.contains("SUBTOTAL")) {
            return false;
        }
        if (upper.contains("TAX TOTAL") || upper.contains("TAXTOTAL")) {
            return true;
        }
        if (upper.contains("TOTAL") && upper.contains("TAX") && !upper.contains("SUB")) {
            return false;
        }
        return upper.contains("TAX")
                || upper.contains("GST")
                || upper.contains("VAT")
                || upper.contains("HST")
                || upper.contains("SALES TAX");
    }

    private static boolean isAmountPaidLine(String upper) {
        return upper.contains("AMOUNT PAID")
                || upper.contains("PAYMENT")
                || upper.contains("CARD PAYMENT")
                || upper.contains("PAID:");
    }

    private static boolean isGrandTotalLine(String upper) {
        if (upper.contains("SUBTOTAL") || upper.contains("SUB TOTAL")) {
            return false;
        }
        if (isTaxAmountLine(upper) && !upper.contains("TOTAL") && !upper.contains("DUE")) {
            return false;
        }
        return upper.contains("GRAND TOTAL")
                || upper.contains("TOTAL DUE")
                || upper.contains("BALANCE DUE")
                || upper.contains("AMOUNT DUE")
                || upper.contains("TOTAL:")
                || upper.contains("ORDER TOTAL")
                || (upper.contains("TOTAL") && !upper.contains("SUB") && !upper.contains("TAX RATE"));
    }

    private static BigDecimal firstMoney(String line) {
        Matcher matcher = MONEY_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        try {
            String token = matcher.group();
            return new BigDecimal(token.replace("$", "").replace(",", "").trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public record ParsedNumericSummary(
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal total,
            BigDecimal amountPaid,
            BigDecimal taxRatePercent
    ) {
    }

    /** @deprecated Use {@link ParsedNumericSummary} */
    @Deprecated
    public record ParsedSummary(
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal total,
            BigDecimal amountPaid
    ) {
    }

    @Deprecated
    ParsedSummary parseLabeledAmounts(String text) {
        ParsedNumericSummary p = parseNumericSummary(text);
        return new ParsedSummary(p.subtotal(), p.tax(), p.total(), p.amountPaid());
    }
}
