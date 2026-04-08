package com.roofingcrm.service.accounting;

import com.roofingcrm.domain.enums.ReceiptAmountConfidence;
import com.roofingcrm.domain.enums.ReceiptFieldConfidence;
import com.roofingcrm.domain.enums.ReceiptTotalSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ReceiptExtractionDecisionService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptExtractionDecisionService.class);

    private static final BigDecimal TOLERANCE = new BigDecimal("0.02");
    private static final BigDecimal TAX_RATE_TOLERANCE = new BigDecimal("0.06");

    public ReceiptAmountDecision decideAmount(ReceiptAmountCandidateExtractor.CandidateExtractionResult candidates,
                                              ReceiptExtractionClient.ExtractedReceiptData fullResult,
                                              SummaryFieldConsensusService.SummaryConsensusResult summaryConsensus) {
        return decideAmount(candidates, fullResult, summaryConsensus, false, null);
    }

    /**
     * @param suppressFullImageNumericEvidence when true, full-image vision numeric fields are cleared for reconciliation
     *                                         (tests / legacy). The default pipeline uses vision-only full-image amounts
     *                                         (never merged with regex/OCR summary line parsing).
     */
    public ReceiptAmountDecision decideAmount(ReceiptAmountCandidateExtractor.CandidateExtractionResult candidates,
                                              ReceiptExtractionClient.ExtractedReceiptData fullResult,
                                              SummaryFieldConsensusService.SummaryConsensusResult summaryConsensus,
                                              boolean suppressFullImageNumericEvidence) {
        return decideAmount(candidates, fullResult, summaryConsensus, suppressFullImageNumericEvidence, null);
    }

    /**
     * @param summaryAttempts per-crop summary vision results; used to pick the best coherent total and to block
     *                        noisy full-image totals when any summary attempt is internally math-consistent.
     */
    public ReceiptAmountDecision decideAmount(ReceiptAmountCandidateExtractor.CandidateExtractionResult candidates,
                                              ReceiptExtractionClient.ExtractedReceiptData fullResult,
                                              SummaryFieldConsensusService.SummaryConsensusResult summaryConsensus,
                                              boolean suppressFullImageNumericEvidence,
                                              List<SummaryFieldConsensusService.SummaryExtractionAttempt> summaryAttempts) {
        BestSummaryAttemptForTotal best = chooseBestSummaryAttemptForTotal(summaryAttempts);
        boolean usedBestAttempt = best.kind() != BestSummaryAttemptKind.NONE;
        boolean blockFullVisionFromCoherentSummary = hasAnyCoherentMathAttempt(summaryAttempts);

        String bestLabel = best.kind() == BestSummaryAttemptKind.NONE ? "none"
                : best.attempt().cropId() + "/" + best.attempt().variantId();
        log.info("[receipt-total-debug] chooseBestSummaryAttemptForTotal label={} kind={} score={}",
                bestLabel, best.kind(), best.score());

        if (best.kind() == BestSummaryAttemptKind.NONE
                && summaryAttempts != null
                && !summaryAttempts.isEmpty()) {
            log.info("[receipt-total-debug] no coherent summary attempt found; fell back to consensus summary source");
        }

        List<String> warnings = new ArrayList<>(candidates.warnings());
        warnings.addAll(summaryConsensus.notes());

        SourceAmounts summarySource = buildEffectiveSummarySource(summaryConsensus, best);
        // Full-image amounts come only from the vision model — never merge regex/OCR line parsing into "full image".
        SourceAmounts fullVision = SourceAmounts.fromFullVisionOnly(fullResult);
        SourceAmounts fullSource = suppressFullImageNumericEvidence ? SourceAmounts.emptyFull() : fullVision;

        addReconciliationWarnings(warnings, summarySource);
        addReconciliationWarnings(warnings, fullSource);
        if (!suppressFullImageNumericEvidence
                && summarySource.total() != null
                && fullSource.total() != null
                && !approximatelyEquals(summarySource.total(), fullSource.total())) {
            warnings.add("Summary region total does not match the full receipt total. Please review before saving.");
        }

        List<ReceiptAmountCandidateExtractor.RankedAmountCandidate> ranked = rerankWithSignals(
                candidates.candidates(),
                suppressFullImageNumericEvidence ? null : fullResult.suggestedAmount(),
                summarySource,
                fullSource
        );

        List<BigDecimal> amountCandidates = buildAmountCandidates(
                ranked,
                summarySource.total(),
                summarySource.computedTotal(),
                fullSource.total(),
                fullSource.computedTotal(),
                summarySource.amountPaid(),
                fullSource.amountPaid(),
                fullResult.suggestedAmount()
        );

        boolean logNoCoherentFallbackToVision = summaryAttempts != null
                && !summaryAttempts.isEmpty()
                && best.kind() == BestSummaryAttemptKind.NONE;

        if (summarySource.hasReliableMathValidatedTotal()) {
            return buildDecision(
                    summarySource.total(),
                    summarySource.subtotal(),
                    summarySource.subtotalConfidence(),
                    summarySource.tax(),
                    summarySource.taxConfidence(),
                    summarySource.total(),
                    ReceiptFieldConfidence.HIGH,
                    summarySource.amountPaid(),
                    summarySource.amountPaidConfidence(),
                    summarySource.computedTotal(),
                    amountCandidates,
                    ReceiptAmountConfidence.HIGH,
                    warnings,
                    mapTotalSource(usedBestAttempt, ReceiptTotalSource.SUMMARY_MATH_VALIDATED)
            );
        }

        if (summarySource.hasReliableSubtotalAndTax()) {
            warnings.add("Using computed total from summary subtotal and tax.");
            return buildDecision(
                    summarySource.computedTotal(),
                    summarySource.subtotal(),
                    summarySource.subtotalConfidence(),
                    summarySource.tax(),
                    summarySource.taxConfidence(),
                    summarySource.computedTotal(),
                    ReceiptFieldConfidence.HIGH,
                    summarySource.amountPaid(),
                    summarySource.amountPaidConfidence(),
                    summarySource.computedTotal(),
                    amountCandidates,
                    ReceiptAmountConfidence.HIGH,
                    warnings,
                    mapTotalSource(usedBestAttempt, ReceiptTotalSource.COMPUTED_SUBTOTAL_PLUS_TAX)
            );
        }

        if (summarySource.hasTotalValidatedByAmountPaid()) {
            return buildDecision(
                    summarySource.total(),
                    summarySource.subtotal(),
                    summarySource.subtotalConfidence(),
                    summarySource.tax(),
                    summarySource.taxConfidence(),
                    summarySource.total(),
                    boost(summarySource.totalConfidence()),
                    summarySource.amountPaid(),
                    boost(summarySource.amountPaidConfidence()),
                    summarySource.computedTotal(),
                    amountCandidates,
                    ReceiptAmountConfidence.HIGH,
                    warnings,
                    mapTotalSource(usedBestAttempt, ReceiptTotalSource.SUMMARY_TOTAL_AND_PAID_MATCH)
            );
        }

        if (!blockFullVisionFromCoherentSummary
                && fullSource.total() != null
                && isConsistentWithSummaryEvidence(fullSource.total(), summarySource)) {
            if (logNoCoherentFallbackToVision) {
                log.info("[receipt-total-debug] no coherent summary attempt found; fell back to full-image vision total");
            }
            return buildDecision(
                    fullSource.total(),
                    firstNonNull(summarySource.subtotal(), fullSource.subtotal()),
                    max(summarySource.subtotalConfidence(), fullSource.subtotalConfidence()),
                    firstNonNull(summarySource.tax(), fullSource.tax()),
                    max(summarySource.taxConfidence(), fullSource.taxConfidence()),
                    fullSource.total(),
                    max(summarySource.totalConfidence(), fullSource.totalConfidence()),
                    firstNonNull(summarySource.amountPaid(), fullSource.amountPaid()),
                    max(summarySource.amountPaidConfidence(), fullSource.amountPaidConfidence()),
                    firstNonNull(summarySource.computedTotal(), fullSource.computedTotal()),
                    amountCandidates,
                    ReceiptAmountConfidence.MEDIUM,
                    warnings,
                    ReceiptTotalSource.VISION_CONSISTENT_WITH_SUMMARY
            );
        }

        if (!blockFullVisionFromCoherentSummary
                && fullSource.total() != null
                && fullSource.totalConfidence().ordinal() >= ReceiptFieldConfidence.MEDIUM.ordinal()
                && shouldUseFullVisionTotal(fullSource, summarySource)) {
            if (logNoCoherentFallbackToVision) {
                log.info("[receipt-total-debug] no coherent summary attempt found; fell back to full-image vision total");
            }
            return buildDecision(
                    fullSource.total(),
                    firstNonNull(summarySource.subtotal(), fullSource.subtotal()),
                    max(summarySource.subtotalConfidence(), fullSource.subtotalConfidence()),
                    firstNonNull(summarySource.tax(), fullSource.tax()),
                    max(summarySource.taxConfidence(), fullSource.taxConfidence()),
                    fullSource.total(),
                    max(summarySource.totalConfidence(), fullSource.totalConfidence()),
                    firstNonNull(summarySource.amountPaid(), fullSource.amountPaid()),
                    max(summarySource.amountPaidConfidence(), fullSource.amountPaidConfidence()),
                    firstNonNull(summarySource.computedTotal(), fullSource.computedTotal()),
                    amountCandidates,
                    ReceiptAmountConfidence.MEDIUM,
                    warnings,
                    ReceiptTotalSource.VISION_TOTAL
            );
        }

        if (!ranked.isEmpty()) {
            warnings.add("Line-based amount candidates were not used for the final total (stabilization); please review.");
        }

        if (!suppressFullImageNumericEvidence
                && canUseVisionAmountPaidAsTotal(fullSource, summarySource, blockFullVisionFromCoherentSummary)) {
            return buildDecision(
                    fullSource.amountPaid(),
                    firstNonNull(summarySource.subtotal(), fullSource.subtotal()),
                    max(summarySource.subtotalConfidence(), fullSource.subtotalConfidence()),
                    firstNonNull(summarySource.tax(), fullSource.tax()),
                    max(summarySource.taxConfidence(), fullSource.taxConfidence()),
                    fullSource.amountPaid(),
                    max(summarySource.totalConfidence(), fullSource.amountPaidConfidence()),
                    fullSource.amountPaid(),
                    fullSource.amountPaidConfidence(),
                    firstNonNull(summarySource.computedTotal(), fullSource.computedTotal()),
                    amountCandidates,
                    ReceiptAmountConfidence.MEDIUM,
                    warnings,
                    ReceiptTotalSource.VISION_AMOUNT_PAID
            );
        }

        if (!suppressFullImageNumericEvidence && fullResult.suggestedAmount() != null) {
            warnings.add("The detected text did not produce a strong total. Please review the suggested amount carefully.");
            return buildDecision(
                    fullResult.suggestedAmount(),
                    null,
                    ReceiptFieldConfidence.UNKNOWN,
                    null,
                    ReceiptFieldConfidence.UNKNOWN,
                    null,
                    ReceiptFieldConfidence.UNKNOWN,
                    null,
                    ReceiptFieldConfidence.UNKNOWN,
                    summarySource.computedTotal(),
                    List.of(fullResult.suggestedAmount()),
                    ReceiptAmountConfidence.LOW,
                    warnings,
                    ReceiptTotalSource.SUGGESTED_AMOUNT_LOW
            );
        }
        warnings.add("Please review the total before saving. We could not detect a reliable amount.");
        return buildDecision(
                null,
                summarySource.subtotal(),
                summarySource.subtotalConfidence(),
                summarySource.tax(),
                summarySource.taxConfidence(),
                summarySource.total(),
                summarySource.totalConfidence(),
                summarySource.amountPaid(),
                summarySource.amountPaidConfidence(),
                summarySource.computedTotal(),
                List.of(),
                ReceiptAmountConfidence.LOW,
                warnings,
                ReceiptTotalSource.NONE
        );
    }

    /**
     * Scores summary crops for TOTAL only: prefers math-coherent subtotal+tax+total, then subtotal+tax matching amount paid
     * when total line is missing.
     */
    public BestSummaryAttemptForTotal chooseBestSummaryAttemptForTotal(
            List<SummaryFieldConsensusService.SummaryExtractionAttempt> attempts) {
        if (attempts == null || attempts.isEmpty()) {
            return new BestSummaryAttemptForTotal(null, BestSummaryAttemptKind.NONE, -1);
        }
        SummaryFieldConsensusService.SummaryExtractionAttempt bestCoherent = null;
        int bestCoherentScore = -1;
        for (SummaryFieldConsensusService.SummaryExtractionAttempt a : attempts) {
            ReceiptExtractionClient.ExtractedReceiptData r = a.result();
            if (!isInternallyMathConsistent(r)) {
                continue;
            }
            int s = 10_000 + confidenceWeight(r.confidence()) + a.cropWeight() + a.variantWeight();
            if (isTotalMatchesAmountPaid(r)) {
                s += 1_000;
            }
            if (s > bestCoherentScore) {
                bestCoherentScore = s;
                bestCoherent = a;
            }
        }
        if (bestCoherent != null) {
            return new BestSummaryAttemptForTotal(bestCoherent, BestSummaryAttemptKind.COHERENT_TOTAL, bestCoherentScore);
        }
        SummaryFieldConsensusService.SummaryExtractionAttempt bestPaid = null;
        int bestPaidScore = -1;
        for (SummaryFieldConsensusService.SummaryExtractionAttempt a : attempts) {
            ReceiptExtractionClient.ExtractedReceiptData r = a.result();
            if (!isAmountPaidOnlyCoherent(r)) {
                continue;
            }
            int s = 5_000 + confidenceWeight(r.confidence()) + a.cropWeight() + a.variantWeight();
            if (s > bestPaidScore) {
                bestPaidScore = s;
                bestPaid = a;
            }
        }
        if (bestPaid != null) {
            return new BestSummaryAttemptForTotal(bestPaid, BestSummaryAttemptKind.AMOUNT_PAID_SUBTAX_MATCH, bestPaidScore);
        }
        return new BestSummaryAttemptForTotal(null, BestSummaryAttemptKind.NONE, -1);
    }

    public enum BestSummaryAttemptKind {
        COHERENT_TOTAL,
        AMOUNT_PAID_SUBTAX_MATCH,
        NONE
    }

    public record BestSummaryAttemptForTotal(
            SummaryFieldConsensusService.SummaryExtractionAttempt attempt,
            BestSummaryAttemptKind kind,
            int score
    ) {
    }

    private SourceAmounts buildEffectiveSummarySource(
            SummaryFieldConsensusService.SummaryConsensusResult consensus,
            BestSummaryAttemptForTotal best) {
        if (best.kind() == BestSummaryAttemptKind.COHERENT_TOTAL) {
            return fromBestSummaryAttempt(best.attempt());
        }
        if (best.kind() == BestSummaryAttemptKind.AMOUNT_PAID_SUBTAX_MATCH) {
            return fromBestSummaryAttemptAmountPaidAsTotal(best.attempt());
        }
        return SourceAmounts.fromSummary(consensus);
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

    private SourceAmounts fromBestSummaryAttempt(SummaryFieldConsensusService.SummaryExtractionAttempt attempt) {
        ReceiptExtractionClient.ExtractedReceiptData r = attempt.result();
        Integer oc = r.confidence();
        return new SourceAmounts(
                "summary region (best attempt)",
                r.subtotal(),
                coherentFieldConfidence(r.subtotal(), oc),
                r.tax(),
                coherentFieldConfidence(r.tax(), oc),
                r.total(),
                isTotalMatchesAmountPaid(r) ? ReceiptFieldConfidence.HIGH : coherentFieldConfidence(r.total(), oc),
                r.amountPaid(),
                coherentFieldConfidence(r.amountPaid(), oc)
        );
    }

    private SourceAmounts fromBestSummaryAttemptAmountPaidAsTotal(SummaryFieldConsensusService.SummaryExtractionAttempt attempt) {
        ReceiptExtractionClient.ExtractedReceiptData r = attempt.result();
        Integer oc = r.confidence();
        BigDecimal total = r.amountPaid();
        return new SourceAmounts(
                "summary region (best attempt)",
                r.subtotal(),
                coherentFieldConfidence(r.subtotal(), oc),
                r.tax(),
                coherentFieldConfidence(r.tax(), oc),
                total,
                ReceiptFieldConfidence.HIGH,
                r.amountPaid(),
                coherentFieldConfidence(r.amountPaid(), oc)
        );
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

    private boolean isAmountPaidOnlyCoherent(ReceiptExtractionClient.ExtractedReceiptData r) {
        if (r.total() != null) {
            return false;
        }
        if (r.subtotal() == null || r.tax() == null || r.amountPaid() == null) {
            return false;
        }
        BigDecimal sum = r.subtotal().add(r.tax()).setScale(2, RoundingMode.HALF_UP);
        return approximatelyEquals(sum, r.amountPaid());
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

    private boolean hasAnyCoherentMathAttempt(List<SummaryFieldConsensusService.SummaryExtractionAttempt> attempts) {
        if (attempts == null || attempts.isEmpty()) {
            return false;
        }
        return attempts.stream().anyMatch(a -> isInternallyMathConsistent(a.result()));
    }

    private static ReceiptTotalSource mapTotalSource(boolean usedBestAttempt, ReceiptTotalSource base) {
        if (usedBestAttempt
                && (base == ReceiptTotalSource.SUMMARY_MATH_VALIDATED
                || base == ReceiptTotalSource.COMPUTED_SUBTOTAL_PLUS_TAX
                || base == ReceiptTotalSource.SUMMARY_TOTAL_AND_PAID_MATCH)) {
            return ReceiptTotalSource.BEST_SUMMARY_COHERENT_ATTEMPT;
        }
        return base;
    }

    /**
     * Full-image vision total is only used when it matches summary subtotal+tax arithmetic when that arithmetic
     * exists, or when summary has no subtotal+tax pair. Blocks noisy full-image totals that disagree with
     * summary consensus (e.g. wrong line read as TOTAL).
     */
    private boolean shouldUseFullVisionTotal(SourceAmounts full, SourceAmounts summary) {
        if (summary.computedTotal() == null) {
            return true;
        }
        return approximatelyEquals(full.total(), summary.computedTotal());
    }

    /**
     * Use vision amount paid as primary only when there are no regex line candidates and summary math does not imply a total.
     */
    private static boolean canUseVisionAmountPaidAsTotal(SourceAmounts fullVision,
                                                         SourceAmounts summary,
                                                         boolean blockWhenCoherentSummaryExists) {
        if (blockWhenCoherentSummaryExists) {
            return false;
        }
        if (fullVision.total() != null) {
            return false;
        }
        if (fullVision.amountPaid() == null
                || fullVision.amountPaidConfidence().ordinal() < ReceiptFieldConfidence.MEDIUM.ordinal()) {
            return false;
        }
        if (summary.total() != null) {
            return false;
        }
        return summary.computedTotal() == null;
    }

    private List<ReceiptAmountCandidateExtractor.RankedAmountCandidate> rerankWithSignals(
            List<ReceiptAmountCandidateExtractor.RankedAmountCandidate> candidates,
            BigDecimal suggestedAmount,
            SourceAmounts summarySource,
            SourceAmounts fullSource) {
        List<ReceiptAmountCandidateExtractor.RankedAmountCandidate> reranked = new ArrayList<>();
        for (ReceiptAmountCandidateExtractor.RankedAmountCandidate candidate : candidates) {
            int score = candidate.score();
            if (suggestedAmount != null && approximatelyEquals(candidate.amount(), suggestedAmount)) {
                score += 10;
            }
            score += scoreForSource(candidate.amount(), summarySource, 170, 130, 90);
            score += scoreForSource(candidate.amount(), fullSource, 110, 90, 60);
            reranked.add(new ReceiptAmountCandidateExtractor.RankedAmountCandidate(
                    candidate.amount(),
                    score,
                    candidate.lineText(),
                    candidate.lineIndex()
            ));
        }
        reranked.sort((left, right) -> {
            int scoreCompare = Integer.compare(right.score(), left.score());
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return Integer.compare(right.lineIndex(), left.lineIndex());
        });
        return reranked;
    }

    private int scoreForSource(BigDecimal amount,
                               SourceAmounts source,
                               int validatedTotalScore,
                               int computedTotalScore,
                               int amountPaidScore) {
        int score = 0;
        if (source.hasReliableMathValidatedTotal() && approximatelyEquals(amount, source.total())) {
            score += validatedTotalScore;
        } else if (source.total() != null && approximatelyEquals(amount, source.total())) {
            score += computedTotalScore;
        }
        if (source.computedTotal() != null && approximatelyEquals(amount, source.computedTotal())) {
            score += computedTotalScore;
        }
        if (source.amountPaid() != null && approximatelyEquals(amount, source.amountPaid())) {
            score += amountPaidScore;
        }
        return score;
    }

    private List<BigDecimal> buildAmountCandidates(
            List<ReceiptAmountCandidateExtractor.RankedAmountCandidate> ranked,
            BigDecimal summaryTotal,
            BigDecimal summaryComputedTotal,
            BigDecimal fullTotal,
            BigDecimal fullComputedTotal,
            BigDecimal summaryAmountPaid,
            BigDecimal fullAmountPaid,
            BigDecimal suggestedAmount) {
        LinkedHashSet<BigDecimal> amounts = new LinkedHashSet<>();
        addIfPresent(amounts, summaryTotal);
        addIfPresent(amounts, summaryComputedTotal);
        addIfPresent(amounts, fullTotal);
        addIfPresent(amounts, fullComputedTotal);
        addIfPresent(amounts, summaryAmountPaid);
        addIfPresent(amounts, fullAmountPaid);
        for (ReceiptAmountCandidateExtractor.RankedAmountCandidate candidate : ranked) {
            addIfPresent(amounts, candidate.amount());
        }
        addIfPresent(amounts, suggestedAmount);
        return new ArrayList<>(amounts);
    }

    private void addIfPresent(LinkedHashSet<BigDecimal> amounts, BigDecimal value) {
        if (value != null) {
            amounts.add(value.setScale(2, RoundingMode.HALF_UP));
        }
    }

    private boolean isConsistentWithSummaryEvidence(BigDecimal candidateTotal, SourceAmounts summarySource) {
        return approximatelyEquals(candidateTotal, summarySource.total())
                || approximatelyEquals(candidateTotal, summarySource.computedTotal())
                || approximatelyEquals(candidateTotal, summarySource.amountPaid());
    }

    private void addReconciliationWarnings(List<String> warnings, SourceAmounts source) {
        if (source.total() != null && source.computedTotal() != null && !approximatelyEquals(source.total(), source.computedTotal())) {
            warnings.add(capitalize(source.sourceName()) + " subtotal plus tax does not match the extracted total.");
        }
        if (source.total() != null && source.amountPaid() != null && !approximatelyEquals(source.total(), source.amountPaid())) {
            warnings.add(capitalize(source.sourceName()) + " amount paid does not match the extracted total.");
        }
    }

    private ReceiptAmountDecision buildDecision(BigDecimal amount,
                                                BigDecimal subtotal,
                                                ReceiptFieldConfidence subtotalConfidence,
                                                BigDecimal tax,
                                                ReceiptFieldConfidence taxConfidence,
                                                BigDecimal total,
                                                ReceiptFieldConfidence totalConfidence,
                                                BigDecimal amountPaid,
                                                ReceiptFieldConfidence amountPaidConfidence,
                                                BigDecimal computedTotal,
                                                List<BigDecimal> amountCandidates,
                                                ReceiptAmountConfidence amountConfidence,
                                                List<String> warnings,
                                                ReceiptTotalSource totalSource) {
        ReceiptAmountDecision preliminary = new ReceiptAmountDecision(
                amount,
                subtotal,
                subtotalConfidence,
                tax,
                taxConfidence,
                total,
                totalConfidence,
                amountPaid,
                amountPaidConfidence,
                computedTotal,
                amountCandidates,
                amountConfidence,
                false,
                null,
                ReceiptFieldConfidence.UNKNOWN,
                false,
                dedupeWarnings(warnings),
                totalSource
        );
        ReceiptAmountDecision afterFinalTax = applyFinalTaxFromSubtotalAndTotal(preliminary);
        ReceiptAmountDecision afterRate = applyDerivedTaxRate(afterFinalTax);
        return applyDeterministicSubtotalTaxRateReconciliation(afterRate);
    }

    /**
     * Uses the final chosen subtotal and total (not intermediate summary rows). Amount paid is not a driver.
     */
    private ReceiptAmountDecision applyFinalTaxFromSubtotalAndTotal(ReceiptAmountDecision d) {
        BigDecimal sub = d.subtotal();
        BigDecimal tot = d.total();
        if (sub == null || tot == null) {
            return d;
        }
        ReceiptFieldConfidence weakestDriver = min(d.subtotalConfidence(), d.totalConfidence());
        if (weakestDriver.ordinal() < ReceiptFieldConfidence.MEDIUM.ordinal()) {
            return d;
        }
        if (tot.compareTo(sub) < 0) {
            return d;
        }
        BigDecimal derivedTax = subtract(tot, sub);
        if (derivedTax == null || derivedTax.signum() < 0) {
            return d;
        }
        BigDecimal currentTax = d.tax();
        if (currentTax != null && approximatelyEquals(currentTax, derivedTax)) {
            return d;
        }
        List<String> w = new ArrayList<>(d.warnings());
        w.add("Tax total was derived from subtotal and total.");
        ReceiptFieldConfidence taxConf = derivedTaxConfidenceFromFinalDrivers(
                d.subtotalConfidence(),
                d.totalConfidence());
        BigDecimal computed = sub.add(derivedTax).setScale(2, RoundingMode.HALF_UP);
        return new ReceiptAmountDecision(
                d.amount(),
                sub,
                d.subtotalConfidence(),
                derivedTax,
                taxConf,
                tot,
                d.totalConfidence(),
                d.amountPaid(),
                d.amountPaidConfidence(),
                computed,
                d.amountCandidates(),
                d.amountConfidence(),
                true,
                d.taxRatePercent(),
                d.taxRateConfidence(),
                d.taxRateDerived(),
                dedupeWarnings(w),
                d.totalSource());
    }

    private static ReceiptFieldConfidence derivedTaxConfidenceFromFinalDrivers(
            ReceiptFieldConfidence subtotalConf,
            ReceiptFieldConfidence totalConf) {
        ReceiptFieldConfidence w = subtotalConf.ordinal() <= totalConf.ordinal() ? subtotalConf : totalConf;
        if (w == ReceiptFieldConfidence.HIGH) {
            return ReceiptFieldConfidence.HIGH;
        }
        if (w.ordinal() >= ReceiptFieldConfidence.MEDIUM.ordinal()) {
            return ReceiptFieldConfidence.MEDIUM;
        }
        return ReceiptFieldConfidence.LOW;
    }

    /**
     * Preferred tax rate: (tax / subtotal) * 100 when both are trusted. Does not use amount paid as a driver.
     */
    private ReceiptAmountDecision applyDerivedTaxRate(ReceiptAmountDecision d) {
        BigDecimal sub = d.subtotal();
        BigDecimal tax = d.tax();
        if (sub == null || tax == null || sub.signum() <= 0) {
            return d;
        }
        if (d.subtotalConfidence().ordinal() < ReceiptFieldConfidence.MEDIUM.ordinal()
                || d.taxConfidence().ordinal() < ReceiptFieldConfidence.MEDIUM.ordinal()) {
            return d;
        }
        BigDecimal derivedRate = tax.divide(sub, 10, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(1, RoundingMode.HALF_UP);
        ReceiptFieldConfidence rateConf = derivedTaxRateConfidence(d);
        BigDecimal existing = d.taxRatePercent();
        List<String> w = new ArrayList<>(d.warnings());
        if (existing != null && approximatelyEqualsRate(existing, derivedRate)) {
            return new ReceiptAmountDecision(
                    d.amount(),
                    d.subtotal(),
                    d.subtotalConfidence(),
                    d.tax(),
                    d.taxConfidence(),
                    d.total(),
                    d.totalConfidence(),
                    d.amountPaid(),
                    d.amountPaidConfidence(),
                    d.computedTotal(),
                    d.amountCandidates(),
                    d.amountConfidence(),
                    d.taxDerived(),
                    existing,
                    rateConf,
                    false,
                    dedupeWarnings(w),
                    d.totalSource());
        }
        if (existing != null && !approximatelyEqualsRate(existing, derivedRate)) {
            w.add("Tax rate was derived from subtotal and tax total.");
            return new ReceiptAmountDecision(
                    d.amount(),
                    d.subtotal(),
                    d.subtotalConfidence(),
                    d.tax(),
                    d.taxConfidence(),
                    d.total(),
                    d.totalConfidence(),
                    d.amountPaid(),
                    d.amountPaidConfidence(),
                    d.computedTotal(),
                    d.amountCandidates(),
                    d.amountConfidence(),
                    d.taxDerived(),
                    derivedRate,
                    rateConf,
                    true,
                    dedupeWarnings(w),
                    d.totalSource());
        }
        return new ReceiptAmountDecision(
                d.amount(),
                d.subtotal(),
                d.subtotalConfidence(),
                d.tax(),
                d.taxConfidence(),
                d.total(),
                d.totalConfidence(),
                d.amountPaid(),
                d.amountPaidConfidence(),
                d.computedTotal(),
                d.amountCandidates(),
                d.amountConfidence(),
                d.taxDerived(),
                derivedRate,
                rateConf,
                true,
                dedupeWarnings(w),
                d.totalSource());
    }

    /**
     * Final deterministic pass: when subtotal and tax rate are trusted, derive tax from
     * {@code subtotal * (snappedRate/100)} (rate snapped to nearest 0.5%), then total = subtotal + tax,
     * and conservatively align amount paid when it matched the pre-reconciliation total.
     */
    private ReceiptAmountDecision applyDeterministicSubtotalTaxRateReconciliation(ReceiptAmountDecision d) {
        BigDecimal sub = d.subtotal();
        if (sub == null || sub.signum() <= 0) {
            return d;
        }
        if (d.subtotalConfidence().ordinal() < ReceiptFieldConfidence.MEDIUM.ordinal()) {
            return d;
        }
        BigDecimal ratePercent = d.taxRatePercent();
        ReceiptFieldConfidence rateConf = d.taxRateConfidence();
        if (ratePercent == null || rateConf == null || rateConf.ordinal() < ReceiptFieldConfidence.MEDIUM.ordinal()) {
            return d;
        }

        BigDecimal beforeTotal = d.total();
        BigDecimal beforePaid = d.amountPaid();
        BigDecimal beforeAmount = d.amount();

        BigDecimal effectiveRate = snapTaxRateToNearestHalfPercent(ratePercent);
        BigDecimal derivedTax = sub.multiply(effectiveRate)
                .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal currentTax = d.tax();
        boolean taxReplaced = currentTax == null
                || currentTax.subtract(derivedTax).abs().compareTo(TOLERANCE) > 0;
        BigDecimal newTax = taxReplaced ? derivedTax : currentTax;

        BigDecimal derivedTotal = sub.add(newTax).setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentTotal = d.total();
        boolean totalReplaced = currentTotal == null
                || currentTotal.subtract(derivedTotal).abs().compareTo(TOLERANCE) > 0;
        BigDecimal newTotal = totalReplaced ? derivedTotal : currentTotal;

        boolean rateSnapped = !approximatelyEqualsRate(effectiveRate, ratePercent);

        boolean paidNeedsAlign = beforePaid != null && beforeTotal != null
                && approximatelyEquals(beforePaid, beforeTotal)
                && newTotal != null
                && !approximatelyEquals(beforePaid, newTotal);

        if (!taxReplaced && !totalReplaced && !rateSnapped && !paidNeedsAlign) {
            return d;
        }

        List<String> w = new ArrayList<>(d.warnings());
        ReceiptFieldConfidence taxConf = d.taxConfidence();
        if (taxReplaced) {
            w.add("Tax total was derived from subtotal and tax rate.");
            taxConf = reconciledTaxConfidenceFromSubtotalAndRate(d.subtotalConfidence(), rateConf);
        }

        ReceiptFieldConfidence totalConf = d.totalConfidence();
        if (totalReplaced && newTotal != null) {
            w.add("Total was derived from subtotal and tax.");
            totalConf = min(d.subtotalConfidence(), taxConf);
        }

        BigDecimal newRatePercent = rateSnapped ? effectiveRate : ratePercent;
        boolean taxRateDerived = d.taxRateDerived() || rateSnapped;

        BigDecimal newPaid = beforePaid;
        boolean paidAligned = false;
        if (paidNeedsAlign) {
            w.add("Amount paid was aligned to the reconciled total.");
            newPaid = newTotal;
            paidAligned = true;
        }

        BigDecimal newAmount = resolvePrimaryAmountAfterReconciliation(
                beforeAmount, beforeTotal, beforePaid, newTotal, taxReplaced, totalReplaced, paidAligned);

        BigDecimal computed = sub.add(newTax).setScale(2, RoundingMode.HALF_UP);
        boolean taxDerived = d.taxDerived() || taxReplaced;

        return new ReceiptAmountDecision(
                newAmount,
                sub,
                d.subtotalConfidence(),
                newTax,
                taxConf,
                newTotal,
                totalConf,
                newPaid,
                d.amountPaidConfidence(),
                computed,
                d.amountCandidates(),
                d.amountConfidence(),
                taxDerived,
                newRatePercent,
                rateConf,
                taxRateDerived,
                dedupeWarnings(w),
                d.totalSource());
    }

    private static BigDecimal snapTaxRateToNearestHalfPercent(BigDecimal ratePercent) {
        if (ratePercent == null) {
            return null;
        }
        return ratePercent.multiply(BigDecimal.valueOf(2))
                .setScale(0, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP);
    }

    private static ReceiptFieldConfidence reconciledTaxConfidenceFromSubtotalAndRate(
            ReceiptFieldConfidence subtotalConf,
            ReceiptFieldConfidence rateConf) {
        ReceiptFieldConfidence w = subtotalConf.ordinal() <= rateConf.ordinal() ? subtotalConf : rateConf;
        if (subtotalConf == ReceiptFieldConfidence.HIGH && rateConf == ReceiptFieldConfidence.HIGH) {
            return ReceiptFieldConfidence.HIGH;
        }
        if (w.ordinal() >= ReceiptFieldConfidence.MEDIUM.ordinal()) {
            return ReceiptFieldConfidence.MEDIUM;
        }
        return ReceiptFieldConfidence.LOW;
    }

    private BigDecimal resolvePrimaryAmountAfterReconciliation(
            BigDecimal beforeAmount,
            BigDecimal beforeTotal,
            BigDecimal beforePaid,
            BigDecimal newTotal,
            boolean taxReplaced,
            boolean totalReplaced,
            boolean paidAligned) {
        if (newTotal == null) {
            return beforeAmount;
        }
        if (beforeAmount == null && (totalReplaced || taxReplaced || paidAligned)) {
            return newTotal;
        }
        if (beforeAmount != null) {
            if (beforeTotal != null
                    && approximatelyEquals(beforeAmount, beforeTotal)
                    && !approximatelyEquals(beforeAmount, newTotal)) {
                return newTotal;
            }
            if (paidAligned && beforePaid != null && approximatelyEquals(beforeAmount, beforePaid)) {
                return newTotal;
            }
        }
        return beforeAmount;
    }

    private ReceiptFieldConfidence derivedTaxRateConfidence(ReceiptAmountDecision d) {
        ReceiptFieldConfidence driver = min(d.subtotalConfidence(), d.taxConfidence());
        if (driver == ReceiptFieldConfidence.HIGH
                && d.subtotalConfidence() == ReceiptFieldConfidence.HIGH
                && d.taxConfidence() == ReceiptFieldConfidence.HIGH) {
            return ReceiptFieldConfidence.HIGH;
        }
        return driver.ordinal() >= ReceiptFieldConfidence.MEDIUM.ordinal()
                ? ReceiptFieldConfidence.MEDIUM
                : ReceiptFieldConfidence.LOW;
    }

    private boolean approximatelyEqualsRate(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.subtract(right).abs().compareTo(TAX_RATE_TOLERANCE) <= 0;
    }

    private static ReceiptFieldConfidence deriveFullFieldConfidence(Integer overallConfidence, BigDecimal value) {
        if (value == null) {
            return ReceiptFieldConfidence.UNKNOWN;
        }
        if (overallConfidence != null && overallConfidence >= 80) {
            return ReceiptFieldConfidence.MEDIUM;
        }
        return ReceiptFieldConfidence.LOW;
    }

    private BigDecimal subtract(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return null;
        }
        return left.subtract(right).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean approximatelyEquals(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.subtract(right).abs().compareTo(TOLERANCE) <= 0;
    }

    private ReceiptFieldConfidence boost(ReceiptFieldConfidence confidence) {
        return switch (confidence) {
            case UNKNOWN -> ReceiptFieldConfidence.LOW;
            case LOW -> ReceiptFieldConfidence.MEDIUM;
            case MEDIUM, HIGH -> ReceiptFieldConfidence.HIGH;
        };
    }

    private ReceiptFieldConfidence max(ReceiptFieldConfidence left, ReceiptFieldConfidence right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }

    private ReceiptFieldConfidence min(ReceiptFieldConfidence left, ReceiptFieldConfidence right) {
        return left.ordinal() <= right.ordinal() ? left : right;
    }

    private static BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    private List<String> dedupeWarnings(List<String> warnings) {
        return new ArrayList<>(new LinkedHashSet<>(warnings.stream()
                .filter(warning -> warning != null && !warning.isBlank())
                .toList()));
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Receipt";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public record ReceiptAmountDecision(
            BigDecimal amount,
            BigDecimal subtotal,
            ReceiptFieldConfidence subtotalConfidence,
            BigDecimal tax,
            ReceiptFieldConfidence taxConfidence,
            BigDecimal total,
            ReceiptFieldConfidence totalConfidence,
            BigDecimal amountPaid,
            ReceiptFieldConfidence amountPaidConfidence,
            BigDecimal computedTotal,
            List<BigDecimal> amountCandidates,
            ReceiptAmountConfidence amountConfidence,
            boolean taxDerived,
            BigDecimal taxRatePercent,
            ReceiptFieldConfidence taxRateConfidence,
            boolean taxRateDerived,
            List<String> warnings,
            ReceiptTotalSource totalSource
    ) {
    }

    private record SourceAmounts(
            String sourceName,
            BigDecimal subtotal,
            ReceiptFieldConfidence subtotalConfidence,
            BigDecimal tax,
            ReceiptFieldConfidence taxConfidence,
            BigDecimal total,
            ReceiptFieldConfidence totalConfidence,
            BigDecimal amountPaid,
            ReceiptFieldConfidence amountPaidConfidence
    ) {
        private static SourceAmounts fromSummary(SummaryFieldConsensusService.SummaryConsensusResult summary) {
            return new SourceAmounts(
                    "summary region",
                    summary.subtotal(),
                    summary.subtotalConfidence(),
                    summary.tax(),
                    summary.taxConfidence(),
                    summary.total(),
                    summary.totalConfidence(),
                    summary.amountPaid(),
                    summary.amountPaidConfidence()
            );
        }

        private static SourceAmounts emptyFull() {
            return new SourceAmounts(
                    "full image",
                    null,
                    ReceiptFieldConfidence.UNKNOWN,
                    null,
                    ReceiptFieldConfidence.UNKNOWN,
                    null,
                    ReceiptFieldConfidence.UNKNOWN,
                    null,
                    ReceiptFieldConfidence.UNKNOWN
            );
        }

        private static SourceAmounts fromFullVisionOnly(ReceiptExtractionClient.ExtractedReceiptData fullResult) {
            return new SourceAmounts(
                    "full image",
                    fullResult.subtotal(),
                    deriveFullFieldConfidence(fullResult.confidence(), fullResult.subtotal()),
                    fullResult.tax(),
                    deriveFullFieldConfidence(fullResult.confidence(), fullResult.tax()),
                    fullResult.total(),
                    deriveFullFieldConfidence(fullResult.confidence(), fullResult.total()),
                    fullResult.amountPaid(),
                    deriveFullFieldConfidence(fullResult.confidence(), fullResult.amountPaid())
            );
        }

        private BigDecimal computedTotal() {
            if (subtotal == null || tax == null) {
                return null;
            }
            return subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
        }

        private boolean hasReliableSubtotalAndTax() {
            return subtotal != null && tax != null
                    && subtotalConfidence.ordinal() >= ReceiptFieldConfidence.MEDIUM.ordinal()
                    && taxConfidence.ordinal() >= ReceiptFieldConfidence.MEDIUM.ordinal();
        }

        private boolean hasReliableMathValidatedTotal() {
            return total != null
                    && hasReliableSubtotalAndTax()
                    && computedTotal() != null
                    && totalConfidence.ordinal() >= ReceiptFieldConfidence.MEDIUM.ordinal()
                    && total.subtract(computedTotal()).abs().compareTo(TOLERANCE) <= 0;
        }

        private boolean hasTotalValidatedByAmountPaid() {
            return total != null
                    && amountPaid != null
                    && totalConfidence.ordinal() >= ReceiptFieldConfidence.MEDIUM.ordinal()
                    && amountPaidConfidence.ordinal() >= ReceiptFieldConfidence.MEDIUM.ordinal()
                    && total.subtract(amountPaid).abs().compareTo(TOLERANCE) <= 0;
        }
    }
}
