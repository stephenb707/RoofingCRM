package com.roofingcrm.storage;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * Safe filename handling for local attachment storage and HTTP download headers.
 */
public final class AttachmentFilenameSanitizer {

    private static final int MAX_FILENAME_LENGTH = 200;

    private AttachmentFilenameSanitizer() {
    }

    /**
     * Validates {@code tenantSlug} as a single relative path segment so {@code baseDir.resolve(tenantSlug)}
     * cannot escape the base directory via traversal or separators.
     */
    public static String sanitizeTenantSlug(String tenantSlug) {
        if (tenantSlug == null || tenantSlug.isBlank()) {
            throw new IllegalArgumentException("Invalid tenant slug");
        }
        String t = tenantSlug.trim();
        if (".".equals(t) || "..".equals(t)) {
            throw new IllegalArgumentException("Invalid tenant slug");
        }
        if (t.contains("\0")) {
            throw new IllegalArgumentException("Invalid tenant slug");
        }
        Path keyPath = Path.of(t);
        if (keyPath.isAbsolute()) {
            throw new IllegalArgumentException("Invalid tenant slug");
        }
        if (keyPath.getNameCount() != 1) {
            throw new IllegalArgumentException("Invalid tenant slug");
        }
        return t;
    }

    /**
     * Produces a safe basename for persisted metadata and on-disk names (path separators and control
     * characters removed). Never empty — falls back to {@code "file"}.
     */
    public static String sanitizeUploadedFilename(String originalFilename) {
        String base = basename(originalFilename);
        String cleaned = stripUnsafeFilenameChars(base);
        cleaned = truncateUtf8Safe(cleaned, MAX_FILENAME_LENGTH);
        if (cleaned.isEmpty()) {
            return "file";
        }
        return cleaned;
    }

    /**
     * Strips header-breaking and problematic characters for use with {@link org.springframework.http.ContentDisposition}.
     */
    public static String sanitizeForContentDisposition(String fileName, UUID attachmentId) {
        Objects.requireNonNull(attachmentId, "attachmentId");
        if (fileName == null || fileName.isBlank()) {
            return attachmentId.toString();
        }
        String base = basename(fileName);
        StringBuilder sb = new StringBuilder(Math.min(base.length(), MAX_FILENAME_LENGTH));
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (c >= 32 && c != '"' && c != '\\' && c != '\r' && c != '\n' && c != '\u007f') {
                sb.append(c);
            }
        }
        String s = sb.toString().strip();
        s = truncateUtf8Safe(s, MAX_FILENAME_LENGTH);
        return s.isEmpty() ? attachmentId.toString() : s;
    }

    private static String basename(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String n = name.replace('\\', '/');
        int idx = n.lastIndexOf('/');
        return idx >= 0 ? n.substring(idx + 1) : n;
    }

    private static String stripUnsafeFilenameChars(String base) {
        if (base.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(base.length());
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (c <= 31 || c == '\u007f') {
                sb.append('_');
                continue;
            }
            if (c == '/' || c == '\\' || c == ':' || c == '*' || c == '?' || c == '"' || c == '<'
                    || c == '>' || c == '|') {
                sb.append('_');
                continue;
            }
            sb.append(c);
        }
        String s = sb.toString().strip();
        while (s.startsWith(".")) {
            s = s.substring(1).strip();
        }
        return s;
    }

    /** Avoid cutting in the middle of a UTF-16 surrogate pair when truncating by char count. */
    private static String truncateUtf8Safe(String s, int maxChars) {
        if (s.length() <= maxChars) {
            return s;
        }
        int end = maxChars;
        if (end > 0 && Character.isHighSurrogate(s.charAt(end - 1))) {
            end--;
        }
        return end <= 0 ? "" : s.substring(0, end);
    }
}
