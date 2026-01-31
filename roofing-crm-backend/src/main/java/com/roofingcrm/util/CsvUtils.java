package com.roofingcrm.util;

/**
 * Utilities for building CSV content with proper escaping.
 */
public final class CsvUtils {

    private CsvUtils() {
    }

    /**
     * Escapes a cell value for CSV output.
     * - null -> ""
     * - If contains comma, quote, newline, or carriage return -> wrap in quotes and escape quotes by doubling
     */
    public static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        if (needsQuoting(s)) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static boolean needsQuoting(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ',' || c == '"' || c == '\n' || c == '\r') {
                return true;
            }
        }
        return false;
    }

    /**
     * UTF-8 BOM for Excel compatibility.
     */
    public static final String UTF8_BOM = "\uFEFF";
}
