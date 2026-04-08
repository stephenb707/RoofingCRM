package com.roofingcrm.service.accounting;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReceiptAmountCandidateExtractor {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.02");

    private static final Pattern MONEY_PATTERN =
            Pattern.compile("(?<!\\d)(?:\\$\\s*)?(\\d{1,3}(?:,\\d{3})*|\\d+)\\.\\d{2}(?!\\d)");

    public CandidateExtractionResult extractCandidates(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new CandidateExtractionResult(
                    List.of(),
                    List.of("No reliable total was detected from the extracted text."),
                    new DetectedSummaryAmounts(null, null, null, null)
            );
        }

        String[] lines = rawText.split("\\R");
        List<LineAmountCandidate> extracted = new ArrayList<>();
        SummaryAmounts summaryAmounts = new SummaryAmounts();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher matcher = MONEY_PATTERN.matcher(line);
            while (matcher.find()) {
                String token = matcher.group();
                BigDecimal amount = parseAmount(token);
                if (amount == null) {
                    continue;
                }
                String normalizedLine = line.toUpperCase(Locale.ROOT);
                int score = 10;
                boolean isSubtotalLine = containsAny(normalizedLine, "SUBTOTAL");
                boolean isTaxLine = containsAny(normalizedLine, "TAX", "GST", "VAT");
                boolean isAmountPaidLine = containsAny(normalizedLine, "AMOUNT PAID", "PAID", "PAYMENT");
                boolean isStrongTotalLine = containsAny(normalizedLine, "GRAND TOTAL", "TOTAL DUE", "BALANCE DUE", "AMOUNT DUE");
                boolean isGenericTotalLine = containsAny(normalizedLine, "TOTAL");
                boolean isItemLine = containsAny(normalizedLine, "ITEM", "SKU", "QTY", "RATE", "UNIT PRICE", "PRICE", "EACH");
                boolean hasMultipleAmounts = countMoneyTokens(line) > 1;

                if (isStrongTotalLine) score += 110;
                if (isGenericTotalLine && !isSubtotalLine && !isTaxLine) score += 55;
                if (isAmountPaidLine) score += 85;

                if (isSubtotalLine) {
                    score -= 70;
                    summaryAmounts.subtotal = amount;
                }
                if (isTaxLine) {
                    score -= 65;
                    summaryAmounts.tax = amount;
                }
                if ((isStrongTotalLine || (isGenericTotalLine && !isSubtotalLine && !isTaxLine))) {
                    summaryAmounts.total = amount;
                }
                if (isAmountPaidLine) {
                    summaryAmounts.amountPaid = amount;
                }
                if (containsAny(normalizedLine, "CHANGE")) score -= 60;
                if (containsAny(normalizedLine, "TIP")) score -= 35;
                if (containsAny(normalizedLine, "DISCOUNT", "SAVINGS")) score -= 45;
                if (containsAny(normalizedLine, "DEPOSIT")) score -= 25;
                if (containsAny(normalizedLine, "TAX TOTAL")) score -= 40;
                if (isItemLine) score -= 35;
                if (hasMultipleAmounts && !isStrongTotalLine && !isAmountPaidLine) score -= 30;
                if (containsAny(normalizedLine, "AMOUNT") && !isStrongTotalLine && !isAmountPaidLine) score -= 15;

                double lineRatio = lines.length <= 1 ? 1d : (double) i / (double) (lines.length - 1);
                score += (int) Math.round(lineRatio * 35d);

                extracted.add(new LineAmountCandidate(amount, score, line, i));
            }
        }

        if (summaryAmounts.subtotal != null && summaryAmounts.tax != null) {
            BigDecimal expectedTotal = summaryAmounts.subtotal.add(summaryAmounts.tax).setScale(2, RoundingMode.HALF_UP);
            for (LineAmountCandidate candidate : extracted) {
                if (candidate.amount().subtract(expectedTotal).abs().compareTo(TOLERANCE) <= 0) {
                    candidate.boost(45);
                }
            }
            if (summaryAmounts.total == null) {
                summaryAmounts.total = expectedTotal;
            }
        }

        Map<BigDecimal, RankedAmountCandidate> merged = new LinkedHashMap<>();
        extracted.stream()
                .sorted(Comparator.comparingInt(LineAmountCandidate::score).reversed()
                        .thenComparing(Comparator.comparingInt(LineAmountCandidate::lineIndex).reversed()))
                .forEach(candidate -> merged.merge(
                        candidate.amount(),
                        new RankedAmountCandidate(candidate.amount(), candidate.score(), candidate.lineText(), candidate.lineIndex()),
                        RankedAmountCandidate::merge
                ));

        List<RankedAmountCandidate> ranked = merged.values().stream()
                .sorted(Comparator.comparingInt(RankedAmountCandidate::score).reversed()
                        .thenComparing(Comparator.comparingInt(RankedAmountCandidate::lineIndex).reversed()))
                .toList();

        List<String> warnings = new ArrayList<>();
        if (ranked.size() > 1
                && (ranked.getFirst().score() - ranked.get(1).score() < 15 || ranked.get(1).score() >= 50)) {
            warnings.add("Multiple possible totals detected. Please confirm before saving.");
        }
        if (ranked.isEmpty()) {
            warnings.add("No reliable total was detected from the extracted text.");
        }

        return new CandidateExtractionResult(ranked, warnings, summaryAmounts.toDetectedSummary());
    }

    private static BigDecimal parseAmount(String token) {
        try {
            return new BigDecimal(token.replace("$", "").replace(",", "").trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean containsAny(String line, String... labels) {
        for (String label : labels) {
            if (line.contains(label)) {
                return true;
            }
        }
        return false;
    }

    private static int countMoneyTokens(String line) {
        int count = 0;
        Matcher matcher = MONEY_PATTERN.matcher(line);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public record CandidateExtractionResult(
            List<RankedAmountCandidate> candidates,
            List<String> warnings,
            DetectedSummaryAmounts summaryAmounts
    ) {
    }

    public record DetectedSummaryAmounts(
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal total,
            BigDecimal amountPaid
    ) {
    }

    public record RankedAmountCandidate(
            BigDecimal amount,
            int score,
            String lineText,
            int lineIndex
    ) {
        private RankedAmountCandidate merge(RankedAmountCandidate other) {
            return this.score >= other.score ? this : other;
        }
    }

    private static final class LineAmountCandidate {
        private final BigDecimal amount;
        private int score;
        private final String lineText;
        private final int lineIndex;

        private LineAmountCandidate(BigDecimal amount, int score, String lineText, int lineIndex) {
            this.amount = amount;
            this.score = score;
            this.lineText = lineText;
            this.lineIndex = lineIndex;
        }

        private BigDecimal amount() {
            return amount;
        }

        private int score() {
            return score;
        }

        private String lineText() {
            return lineText;
        }

        private int lineIndex() {
            return lineIndex;
        }

        private void boost(int value) {
            score += value;
        }
    }

    private static final class SummaryAmounts {
        private BigDecimal subtotal;
        private BigDecimal tax;
        private BigDecimal total;
        private BigDecimal amountPaid;

        private DetectedSummaryAmounts toDetectedSummary() {
            return new DetectedSummaryAmounts(subtotal, tax, total, amountPaid);
        }
    }
}
