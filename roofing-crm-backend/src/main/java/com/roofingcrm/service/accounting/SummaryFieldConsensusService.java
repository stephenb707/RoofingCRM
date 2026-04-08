package com.roofingcrm.service.accounting;

import com.roofingcrm.domain.enums.ReceiptFieldConfidence;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
public class SummaryFieldConsensusService {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.02");

    public SummaryConsensusResult buildConsensus(List<SummaryExtractionAttempt> attempts) {
        FieldConsensus subtotal = consensus(attempts, SummaryField.SUBTOTAL);
        FieldConsensus tax = consensus(attempts, SummaryField.TAX);
        FieldConsensus total = consensus(attempts, SummaryField.TOTAL);
        FieldConsensus amountPaid = consensus(attempts, SummaryField.AMOUNT_PAID);

        List<String> rawTexts = new ArrayList<>();
        for (SummaryExtractionAttempt attempt : attempts) {
            String rawText = normalize(attempt.result().rawExtractedText());
            if (rawText != null) {
                rawTexts.add("[%s/%s] %s".formatted(attempt.cropId(), attempt.variantId(), rawText));
            }
        }

        List<String> notes = new ArrayList<>();
        if (total.value() != null && subtotal.value() != null && tax.value() != null
                && !approximatelyEquals(total.value(), subtotal.value().add(tax.value()).setScale(2, RoundingMode.HALF_UP))) {
            notes.add("Summary-region consensus total differs from subtotal plus tax.");
        }

        SummaryConsensusResult draft = new SummaryConsensusResult(
                subtotal.value(),
                subtotal.confidence(),
                tax.value(),
                tax.confidence(),
                total.value(),
                total.confidence(),
                amountPaid.value(),
                amountPaid.confidence(),
                combineRawText(rawTexts),
                notes
        );
        return applyCoherentSummaryAttemptOverride(attempts, draft);
    }

    /**
     * When per-field voting picks a noisy total, prefer a single summary attempt whose subtotal+tax+total agree
     * internally (and optionally total≈amount paid). This restores behavior that worked on real receipts before
     * consensus diluted a correct crop with a wrong one.
     */
    private SummaryConsensusResult applyCoherentSummaryAttemptOverride(
            List<SummaryExtractionAttempt> attempts,
            SummaryConsensusResult base) {
        SummaryExtractionAttempt best = null;
        int bestScore = -1;
        for (SummaryExtractionAttempt attempt : attempts) {
            ReceiptExtractionClient.ExtractedReceiptData r = attempt.result();
            if (!isInternallyMathConsistent(r)) {
                continue;
            }
            int score = 10_000;
            if (isTotalMatchesAmountPaid(r)) {
                score += 1_000;
            }
            score += attempt.cropWeight() + attempt.variantWeight() + confidenceWeight(r.confidence());
            if (score > bestScore) {
                bestScore = score;
                best = attempt;
            }
        }
        if (best == null) {
            return base;
        }
        ReceiptExtractionClient.ExtractedReceiptData r = best.result();
        List<String> notes = new ArrayList<>(base.notes());
        notes.add("Summary totals use coherent attempt %s/%s (subtotal+tax+total aligned on that crop)."
                .formatted(best.cropId(), best.variantId()));
        Integer oc = r.confidence();
        return new SummaryConsensusResult(
                r.subtotal(),
                coherentFieldConfidence(r.subtotal(), oc),
                r.tax(),
                coherentFieldConfidence(r.tax(), oc),
                r.total(),
                isTotalMatchesAmountPaid(r) ? ReceiptFieldConfidence.HIGH : coherentFieldConfidence(r.total(), oc),
                r.amountPaid(),
                coherentFieldConfidence(r.amountPaid(), oc),
                base.rawText(),
                notes
        );
    }

    private static ReceiptFieldConfidence coherentFieldConfidence(BigDecimal value, Integer overallConfidence) {
        if (value == null) {
            return ReceiptFieldConfidence.UNKNOWN;
        }
        if (overallConfidence != null && overallConfidence >= 85) {
            return ReceiptFieldConfidence.HIGH;
        }
        if (overallConfidence != null && overallConfidence >= 70) {
            return ReceiptFieldConfidence.MEDIUM;
        }
        return ReceiptFieldConfidence.MEDIUM;
    }

    private boolean isInternallyMathConsistent(ReceiptExtractionClient.ExtractedReceiptData r) {
        if (r.subtotal() == null || r.tax() == null || r.total() == null) {
            return false;
        }
        BigDecimal sum = r.subtotal().add(r.tax()).setScale(2, RoundingMode.HALF_UP);
        return approximatelyEquals(sum, r.total());
    }

    private boolean isTotalMatchesAmountPaid(ReceiptExtractionClient.ExtractedReceiptData r) {
        if (r.total() == null || r.amountPaid() == null) {
            return false;
        }
        return approximatelyEquals(r.total(), r.amountPaid());
    }

    private FieldConsensus consensus(List<SummaryExtractionAttempt> attempts, SummaryField field) {
        List<ValueGroup> groups = new ArrayList<>();
        for (SummaryExtractionAttempt attempt : attempts) {
            BigDecimal value = field.extract(attempt.result());
            if (value == null) {
                continue;
            }
            ValueGroup matching = null;
            for (ValueGroup group : groups) {
                if (approximatelyEquals(group.value, value)) {
                    matching = group;
                    break;
                }
            }
            int weight = attempt.cropWeight() + attempt.variantWeight() + confidenceWeight(attempt.result().confidence())
                    + labelWeight(field, attempt.result().rawExtractedText());
            if (matching == null) {
                matching = new ValueGroup(value, weight);
                groups.add(matching);
            } else {
                matching.support += weight;
                matching.occurrences += 1;
            }
        }

        if (groups.isEmpty()) {
            return new FieldConsensus(null, ReceiptFieldConfidence.UNKNOWN);
        }

        groups.sort(Comparator.comparingInt(ValueGroup::support).reversed()
                .thenComparing(Comparator.comparingInt(ValueGroup::occurrences).reversed()));

        ValueGroup best = groups.getFirst();
        int nextSupport = groups.size() > 1 ? groups.get(1).support() : 0;
        ReceiptFieldConfidence confidence;
        if (best.occurrences() >= 3 || (best.occurrences() >= 2 && best.support() - nextSupport >= 2)) {
            confidence = ReceiptFieldConfidence.HIGH;
        } else if (best.occurrences() >= 2 || best.support() >= 7) {
            confidence = ReceiptFieldConfidence.MEDIUM;
        } else {
            confidence = ReceiptFieldConfidence.LOW;
        }
        return new FieldConsensus(best.value().setScale(2, RoundingMode.HALF_UP), confidence);
    }

    private int confidenceWeight(Integer confidence) {
        if (confidence == null) {
            return 0;
        }
        if (confidence >= 85) {
            return 3;
        }
        if (confidence >= 70) {
            return 2;
        }
        if (confidence >= 50) {
            return 1;
        }
        return 0;
    }

    private int labelWeight(SummaryField field, String rawText) {
        String normalized = normalize(rawText);
        if (normalized == null) {
            return 0;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        return switch (field) {
            case SUBTOTAL -> upper.contains("SUBTOTAL") ? 2 : 0;
            case TAX -> containsAny(upper, "TAX", "GST", "VAT") ? 2 : 0;
            case TOTAL -> containsAny(upper, "GRAND TOTAL", "TOTAL DUE", "BALANCE DUE", "AMOUNT DUE", "TOTAL") ? 3 : 0;
            case AMOUNT_PAID -> containsAny(upper, "AMOUNT PAID", "PAID", "PAYMENT") ? 2 : 0;
        };
    }

    private boolean containsAny(String value, String... labels) {
        for (String label : labels) {
            if (value.contains(label)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String combineRawText(List<String> values) {
        if (values.isEmpty()) {
            return null;
        }
        return String.join("\n\n", new LinkedHashSet<>(values));
    }

    private boolean approximatelyEquals(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.subtract(right).abs().compareTo(TOLERANCE) <= 0;
    }

    public record SummaryExtractionAttempt(
            String cropId,
            int cropWeight,
            String variantId,
            int variantWeight,
            ReceiptExtractionClient.ExtractedReceiptData result
    ) {
    }

    public record SummaryConsensusResult(
            BigDecimal subtotal,
            ReceiptFieldConfidence subtotalConfidence,
            BigDecimal tax,
            ReceiptFieldConfidence taxConfidence,
            BigDecimal total,
            ReceiptFieldConfidence totalConfidence,
            BigDecimal amountPaid,
            ReceiptFieldConfidence amountPaidConfidence,
            String rawText,
            List<String> notes
    ) {
    }

    public record FieldConsensus(
            BigDecimal value,
            ReceiptFieldConfidence confidence
    ) {
    }

    private enum SummaryField {
        SUBTOTAL,
        TAX,
        TOTAL,
        AMOUNT_PAID;

        private BigDecimal extract(ReceiptExtractionClient.ExtractedReceiptData result) {
            return switch (this) {
                case SUBTOTAL -> result.subtotal();
                case TAX -> result.tax();
                case TOTAL -> result.total();
                case AMOUNT_PAID -> result.amountPaid();
            };
        }
    }

    private static final class ValueGroup {
        private final BigDecimal value;
        private int support;
        private int occurrences;

        private ValueGroup(BigDecimal value, int initialSupport) {
            this.value = value;
            this.support = initialSupport;
            this.occurrences = 1;
        }

        private BigDecimal value() {
            return value;
        }

        private int support() {
            return support;
        }

        private int occurrences() {
            return occurrences;
        }
    }
}
