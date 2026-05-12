package com.roofingcrm.service.customerreport;

import com.roofingcrm.domain.entity.Attachment;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * MIME-aware allowlist for images that appear in customer photo reports and related UI.
 * Excludes vague {@code image/*} edge cases such as SVG (not embedded as raster in PDF) and HEIC
 * where decode support is unreliable without native codecs.
 */
public final class ReportGalleryImageMime {

    private static final Set<String> ALLOWED = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/pjpeg",
            "image/gif",
            "image/webp",
            "image/bmp",
            "image/x-ms-bmp",
            "image/tiff",
            "image/tif",
            "image/x-tiff");

    private ReportGalleryImageMime() {
    }

    /**
     * Returns the subtype part without parameters, lowercase (e.g. {@code image/jpeg}).
     */
    public static String primaryType(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        int semi = trimmed.indexOf(';');
        String part = semi >= 0 ? trimmed.substring(0, semi) : trimmed;
        return part.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isSupported(String contentTypeRaw) {
        return ALLOWED.contains(primaryType(contentTypeRaw));
    }

    public static void requireSupportedReportGalleryImage(Attachment attachment) {
        Objects.requireNonNull(attachment, "attachment");
        if (!isSupported(attachment.getContentType())) {
            throw new IllegalArgumentException(
                    "Report photos must be a supported raster image "
                            + "(PNG, JPEG, GIF, WebP, BMP, or TIFF).");
        }
    }
}
