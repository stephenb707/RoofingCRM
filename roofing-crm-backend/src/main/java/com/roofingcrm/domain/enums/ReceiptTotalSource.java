package com.roofingcrm.domain.enums;

/**
 * Which signal produced the primary receipt total / amount for extraction (debugging).
 */
public enum ReceiptTotalSource {
    /** Summary consensus: subtotal + tax equals total (math-validated). */
    SUMMARY_MATH_VALIDATED,
    /** Computed total from trusted summary subtotal + tax (no explicit total). */
    COMPUTED_SUBTOTAL_PLUS_TAX,
    /** Full-image vision model returned an explicit total (primary vision total). */
    VISION_TOTAL,
    /** Full-image vision: amount paid used as primary when no total and no summary total. */
    VISION_AMOUNT_PAID,
    /** Summary: total matches amount paid with confidence. */
    SUMMARY_TOTAL_AND_PAID_MATCH,
    /** Single math-coherent summary crop/variant chosen for total over per-field consensus. */
    BEST_SUMMARY_COHERENT_ATTEMPT,
    /** Full-image total consistent with summary evidence (legacy branch). */
    VISION_CONSISTENT_WITH_SUMMARY,
    /** Ranked regex candidates from vision text (fallback). */
    CANDIDATE_FALLBACK,
    /** Full-image suggested amount only (low confidence). */
    SUGGESTED_AMOUNT_LOW,
    /** No reliable total. */
    NONE
}
