package com.roofingcrm.service.accounting;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic ranking of dates in vision text (ISO, US slash, month names). No extra provider calls.
 */
@Component
public class ReceiptDateCandidateRanker {

    private static final Pattern ISO_DATE =
            Pattern.compile("\\b(20\\d{2})-(\\d{1,2})-(\\d{1,2})\\b");

    private static final Pattern SLASH_DATE =
            Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})/(\\d{2}|\\d{4})\\b");

    private static final Pattern MONTH_NAME_DATE = Pattern.compile(
            "(?i)\\b(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)"
                    + "\\s+(\\d{1,2})(?:st|nd|rd|th)?,?\\s+(\\d{4})\\b");

    private static final String[] DATE_LABELS = {
            "DATE",
            "ORDER DATE",
            "PICK UP DATE",
            "DELIVERY DATE",
            "INVOICE DATE",
            "SALE DATE",
            "TRANSACTION DATE",
            "PICKUP DATE"
    };

    private final Clock clock;

    public ReceiptDateCandidateRanker() {
        this(Clock.systemUTC());
    }

    ReceiptDateCandidateRanker(Clock clock) {
        this.clock = clock;
    }

    /**
     * Picks the best-incurred date from the primary vision string plus dates found in combined raw text.
     * Does not invent a date when nothing parses.
     */
    public Instant resolveIncurredAt(String preferredIsoDate, String combinedRawText) {
        LocalDate preferred = parseFlexibleDate(preferredIsoDate);
        int currentYear = LocalDate.now(clock).getYear();

        Map<LocalDate, Integer> bestScore = new LinkedHashMap<>();
        if (preferred != null) {
            bestScore.put(preferred, scoreVisionPrimary(preferred, currentYear));
        }
        if (combinedRawText != null && !combinedRawText.isBlank()) {
            collectIsoMatches(combinedRawText, currentYear, bestScore);
            collectSlashMatches(combinedRawText, currentYear, bestScore);
            collectMonthNameMatches(combinedRawText, currentYear, bestScore);
        }

        if (bestScore.isEmpty()) {
            return null;
        }

        return bestScore.entrySet().stream()
                .max(Comparator.<Map.Entry<LocalDate, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey))
                .map(e -> e.getKey().atTime(12, 0).toInstant(ZoneOffset.UTC))
                .orElse(null);
    }

    /**
     * Parses ISO, US slash (M/D/YY[YY]), or month-name forms for the vision primary field.
     */
    public LocalDate parseFlexibleDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String t = value.trim();
        LocalDate iso = tryParseIso(t);
        if (iso != null) {
            return iso;
        }
        Matcher slash = SLASH_DATE.matcher(t);
        if (slash.find()) {
            LocalDate d = parseSlashGroups(slash.group(1), slash.group(2), slash.group(3));
            if (d != null) {
                return d;
            }
        }
        Matcher month = MONTH_NAME_DATE.matcher(t);
        if (month.find()) {
            return parseMonthNameGroups(month.group(1), month.group(2), month.group(3));
        }
        return null;
    }

    private void collectIsoMatches(String raw, int currentYear, Map<LocalDate, Integer> bestScore) {
        Matcher matcher = ISO_DATE.matcher(raw);
        while (matcher.find()) {
            LocalDate d = tryParseIso(matcher.group());
            if (d == null) {
                continue;
            }
            int score = yearPlausibility(d.getYear(), currentYear)
                    + labelProximityScore(raw, matcher.start(), matcher.end());
            bestScore.merge(d, score, Integer::max);
        }
    }

    private void collectSlashMatches(String raw, int currentYear, Map<LocalDate, Integer> bestScore) {
        Matcher matcher = SLASH_DATE.matcher(raw);
        while (matcher.find()) {
            LocalDate d = parseSlashGroups(matcher.group(1), matcher.group(2), matcher.group(3));
            if (d == null) {
                continue;
            }
            int score = yearPlausibility(d.getYear(), currentYear)
                    + labelProximityScore(raw, matcher.start(), matcher.end());
            bestScore.merge(d, score, Integer::max);
        }
    }

    private void collectMonthNameMatches(String raw, int currentYear, Map<LocalDate, Integer> bestScore) {
        Matcher matcher = MONTH_NAME_DATE.matcher(raw);
        while (matcher.find()) {
            LocalDate d = parseMonthNameGroups(matcher.group(1), matcher.group(2), matcher.group(3));
            if (d == null) {
                continue;
            }
            int score = yearPlausibility(d.getYear(), currentYear)
                    + labelProximityScore(raw, matcher.start(), matcher.end())
                    + 2;
            bestScore.merge(d, score, Integer::max);
        }
    }

    private static LocalDate tryParseIso(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private static LocalDate parseSlashGroups(String mStr, String dStr, String yStr) {
        try {
            int month = Integer.parseInt(mStr);
            int day = Integer.parseInt(dStr);
            int year;
            if (yStr.length() == 4) {
                year = Integer.parseInt(yStr);
            } else {
                int yy = Integer.parseInt(yStr);
                year = yy <= 50 ? 2000 + yy : 1900 + yy;
            }
            if (month < 1 || month > 12) {
                return null;
            }
            return LocalDate.of(year, month, day);
        } catch (Exception ex) {
            return null;
        }
    }

    private static LocalDate parseMonthNameGroups(String monthToken, String dayStr, String yearStr) {
        try {
            int month = monthFromName(monthToken);
            if (month < 0) {
                return null;
            }
            int day = Integer.parseInt(dayStr);
            int year = Integer.parseInt(yearStr);
            return LocalDate.of(year, month, day);
        } catch (Exception ex) {
            return null;
        }
    }

    private static int monthFromName(String token) {
        String t = token.toLowerCase(Locale.ROOT);
        if (t.startsWith("jan")) {
            return 1;
        }
        if (t.startsWith("feb")) {
            return 2;
        }
        if (t.startsWith("mar")) {
            return 3;
        }
        if (t.startsWith("apr")) {
            return 4;
        }
        if (t.startsWith("may")) {
            return 5;
        }
        if (t.startsWith("jun")) {
            return 6;
        }
        if (t.startsWith("jul")) {
            return 7;
        }
        if (t.startsWith("aug")) {
            return 8;
        }
        if (t.startsWith("sep")) {
            return 9;
        }
        if (t.startsWith("oct")) {
            return 10;
        }
        if (t.startsWith("nov")) {
            return 11;
        }
        if (t.startsWith("dec")) {
            return 12;
        }
        return -1;
    }

    private static int scoreVisionPrimary(LocalDate d, int currentYear) {
        return 88 + yearPlausibility(d.getYear(), currentYear);
    }

    private static int labelProximityScore(String raw, int matchStart, int matchEnd) {
        int lineStart = raw.lastIndexOf('\n', matchStart);
        lineStart = lineStart < 0 ? 0 : lineStart + 1;
        int lineEnd = raw.indexOf('\n', matchEnd);
        lineEnd = lineEnd < 0 ? raw.length() : lineEnd;
        String line = raw.substring(lineStart, Math.min(lineEnd, raw.length())).toUpperCase(Locale.ROOT);
        int bonus = labelBonus(line);
        if (lineStart > 1) {
            int prevLineStart = raw.lastIndexOf('\n', lineStart - 2);
            prevLineStart = prevLineStart < 0 ? 0 : prevLineStart + 1;
            String prev = raw.substring(prevLineStart, lineStart - 1).toUpperCase(Locale.ROOT);
            bonus = Math.max(bonus, labelBonus(prev));
        }
        return bonus;
    }

    private static int labelBonus(String upperLine) {
        for (String label : DATE_LABELS) {
            if (upperLine.contains(label)) {
                return 52;
            }
        }
        return 0;
    }

    private static int yearPlausibility(int year, int currentYear) {
        if (year < 1990 || year > currentYear + 1) {
            return -130;
        }
        int diff = Math.abs(currentYear - year);
        if (diff == 0) {
            return 42;
        }
        if (diff == 1) {
            return 30;
        }
        if (diff == 2) {
            return 18;
        }
        return Math.max(-45, 22 - diff * 6);
    }
}
