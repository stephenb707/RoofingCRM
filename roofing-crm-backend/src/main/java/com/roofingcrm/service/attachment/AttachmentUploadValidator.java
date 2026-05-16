package com.roofingcrm.service.attachment;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class AttachmentUploadValidator {

    private final AttachmentUploadProperties properties;

    public AttachmentUploadValidator(AttachmentUploadProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    public void validate(@NonNull MultipartFile file, @NonNull AttachmentUploadContext context) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty. Choose a non-empty file to upload.");
        }
        long maxBytes = properties.getMaxFileSize().toBytes();
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(String.format(
                    "File is too large. Maximum size is %s (this file is %,d bytes).",
                    properties.getMaxFileSizeDisplayString(),
                    file.getSize()));
        }
        if (properties.isEnforceContentTypes()) {
            List<String> allowed = allowedList(context);
            String contentType = file.getContentType();
            if (!isAllowedContentType(contentType, allowed)) {
                throw new IllegalArgumentException(String.format(
                        "Unsupported file type for this upload. Content-Type was \"%s\". Allowed types: %s",
                        contentType != null && !contentType.isBlank() ? contentType.trim() : "(missing)",
                        String.join(", ", allowed)));
            }
            try {
                byte[] bytes = file.getBytes();
                UploadContentInspector.verifyBytesMatchDeclaredType(bytes, contentType);
            } catch (IOException e) {
                throw new IllegalArgumentException("Uploaded file could not be read.");
            }
        }
    }

    private List<String> allowedList(AttachmentUploadContext context) {
        return switch (context) {
            case LEAD_ATTACHMENT -> properties.getAllowedContentTypesLead();
            case JOB_ATTACHMENT -> properties.getAllowedContentTypesJob();
            case JOB_RECEIPT -> properties.getAllowedContentTypesReceipt();
        };
    }

    /**
     * Exposed for tests; entries ending with {@code /*} match any subtype of that top-level type.
     */
    static boolean isAllowedContentType(String contentType, List<String> allowedPatterns) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        for (String pattern : allowedPatterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            String p = pattern.trim().toLowerCase(Locale.ROOT);
            if (p.endsWith("/*")) {
                String prefix = p.substring(0, p.length() - 1);
                if (normalized.startsWith(prefix)) {
                    return true;
                }
            } else if (normalized.equals(p)) {
                return true;
            }
        }
        return false;
    }
}
