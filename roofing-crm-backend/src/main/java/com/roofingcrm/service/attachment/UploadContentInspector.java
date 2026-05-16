package com.roofingcrm.service.attachment;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Locale;

/**
 * Lightweight verification that upload bytes match a declared image or PDF type.
 * Client-supplied {@link org.springframework.web.multipart.MultipartFile#getContentType()} is not trusted alone.
 */
final class UploadContentInspector {

    private UploadContentInspector() {
    }

    /**
     * @param data                 file contents (already size-validated)
     * @param declaredContentTypeRaw client-declared MIME, may include parameters
     */
    static void verifyBytesMatchDeclaredType(byte[] data, String declaredContentTypeRaw) {
        if (data == null) {
            throw new IllegalArgumentException("Uploaded file could not be read.");
        }
        String primary = primaryType(declaredContentTypeRaw);
        if (primary.startsWith("image/")) {
            if (!canDecodeAsRasterImage(data)) {
                throw new IllegalArgumentException("Uploaded image could not be read.");
            }
        } else if ("application/pdf".equals(primary)) {
            if (!hasPdfMagic(data)) {
                throw new IllegalArgumentException("Uploaded file content does not match its declared type.");
            }
        }
    }

    static String primaryType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.trim();
        int semi = t.indexOf(';');
        if (semi >= 0) {
            t = t.substring(0, semi);
        }
        return t.trim().toLowerCase(Locale.ROOT);
    }

    static boolean hasPdfMagic(byte[] data) {
        if (data.length < 4) {
            return false;
        }
        return data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F';
    }

    private static boolean canDecodeAsRasterImage(byte[] data) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            return img != null;
        } catch (Exception e) {
            return false;
        }
    }
}
