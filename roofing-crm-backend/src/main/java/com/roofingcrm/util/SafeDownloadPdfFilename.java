package com.roofingcrm.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Builds filesystem-safe downloadable PDF filenames from user-facing titles.
 */
public final class SafeDownloadPdfFilename {

    private static final String DEFAULT_TITLE = "Customer Report";

    /** Windows + general invalid filename characters. */
    private static final Pattern INVALID_CHARS =
            Pattern.compile("[<>:\"/\\\\|?*\\x00-\\x1F]");

    private static final int MAX_STEM_CHARS = 180;

    private SafeDownloadPdfFilename() {
    }

    /**
     * Returns a sanitized filename ending with {@code .pdf} (stem derived from {@code title}).
     */
    public static String fromReportTitle(String title) {
        String stem = stemFromTitle(title);
        if (stem.isBlank()) {
            stem = DEFAULT_TITLE;
        }
        return stem + ".pdf";
    }

    private static String stemFromTitle(String title) {
        if (title == null) {
            return "";
        }
        String t = title.replace('\u00A0', ' ').trim();
        if (t.isBlank()) {
            return "";
        }
        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            t = t.substring(0, t.length() - 4).trim();
        }
        if (t.isBlank()) {
            return "";
        }
        t = t.replaceAll("\\s+", " ");

        String replaced = INVALID_CHARS.matcher(t).replaceAll("-");
        replaced = replaced.replace("\"", "");

        replaced = replaced.replaceAll("-{2,}", "-");
        replaced = stripTrailingDotsAndSpaces(replaced);

        if (replaced.isBlank()) {
            return "";
        }
        if (replaced.length() > MAX_STEM_CHARS) {
            replaced = replaced.substring(0, MAX_STEM_CHARS).trim();
            replaced = stripTrailingDotsAndSpaces(replaced);
        }
        return replaced.isBlank() ? "" : replaced;
    }

    private static String stripTrailingDotsAndSpaces(String s) {
        String out = s;
        while (!out.isEmpty()) {
            char last = out.charAt(out.length() - 1);
            if (last == '.' || last == ' ') {
                out = out.substring(0, out.length() - 1);
            } else {
                break;
            }
        }
        return out;
    }
}
