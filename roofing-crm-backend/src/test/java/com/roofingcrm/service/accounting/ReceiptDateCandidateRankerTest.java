package com.roofingcrm.service.accounting;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReceiptDateCandidateRankerTest {

    @Test
    void prefersLabeledRecentDateOverStaleVisionPrimary() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-01T12:00:00Z"), ZoneOffset.UTC);
        ReceiptDateCandidateRanker ranker = new ReceiptDateCandidateRanker(clock);
        String raw = """
                Invoice Date: 2026-03-28
                Other noise 2023-06-01
                """;
        Instant result = ranker.resolveIncurredAt("2023-06-01", raw);
        assertEquals(LocalDate.of(2026, 3, 28).atTime(12, 0).toInstant(ZoneOffset.UTC), result);
    }

    @Test
    void usesVisionPrimaryWhenNoStrongerCandidateInText() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-01T12:00:00Z"), ZoneOffset.UTC);
        ReceiptDateCandidateRanker ranker = new ReceiptDateCandidateRanker(clock);
        Instant result = ranker.resolveIncurredAt("2026-03-28", "unlabeled 2025-01-01 noise");
        assertEquals(LocalDate.of(2026, 3, 28).atTime(12, 0).toInstant(ZoneOffset.UTC), result);
    }

    @Test
    void returnsNullWhenNoParseableInput() {
        ReceiptDateCandidateRanker ranker = new ReceiptDateCandidateRanker();
        assertNull(ranker.resolveIncurredAt(null, "no iso dates here"));
        assertNull(ranker.resolveIncurredAt("", ""));
    }

    @Test
    void parseFlexibleDate_parsesMmDdYyyy() {
        ReceiptDateCandidateRanker ranker = new ReceiptDateCandidateRanker();
        assertEquals(LocalDate.of(2026, 3, 28), ranker.parseFlexibleDate("03/28/2026"));
    }

    @Test
    void parseFlexibleDate_parsesMDYWithTwoDigitYear() {
        ReceiptDateCandidateRanker ranker = new ReceiptDateCandidateRanker();
        assertEquals(LocalDate.of(2026, 3, 28), ranker.parseFlexibleDate("3/28/26"));
    }

    @Test
    void parseFlexibleDate_parsesMonthNameCommaYear() {
        ReceiptDateCandidateRanker ranker = new ReceiptDateCandidateRanker();
        assertEquals(LocalDate.of(2026, 3, 28), ranker.parseFlexibleDate("March 28, 2026"));
    }

    @Test
    void resolveIncurredAt_findsSlashDateInRawText() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-01T12:00:00Z"), ZoneOffset.UTC);
        ReceiptDateCandidateRanker ranker = new ReceiptDateCandidateRanker(clock);
        Instant result = ranker.resolveIncurredAt(null, "Total due\nDate: 03/28/2026\nThanks");
        assertEquals(LocalDate.of(2026, 3, 28).atTime(12, 0).toInstant(ZoneOffset.UTC), result);
    }

    @Test
    void resolveIncurredAt_isoStillWorksInRawText() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-01T12:00:00Z"), ZoneOffset.UTC);
        ReceiptDateCandidateRanker ranker = new ReceiptDateCandidateRanker(clock);
        Instant result = ranker.resolveIncurredAt(null, "Receipt reference 2026-03-28");
        assertEquals(LocalDate.of(2026, 3, 28).atTime(12, 0).toInstant(ZoneOffset.UTC), result);
    }
}
