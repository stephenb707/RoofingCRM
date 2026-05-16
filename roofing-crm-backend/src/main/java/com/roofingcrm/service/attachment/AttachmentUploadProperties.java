package com.roofingcrm.service.attachment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.ArrayList;
import java.util.List;

/**
 * Upload validation limits. Keep {@link #maxFileSize} aligned with
 * {@code spring.servlet.multipart.max-file-size} so servlet limits and app validation match.
 * <p>
 * Registered via {@link org.springframework.boot.context.properties.EnableConfigurationProperties}
 * on {@link com.roofingcrm.RoofingCrmApplication}.
 */
@ConfigurationProperties(prefix = "app.attachments")
public class AttachmentUploadProperties {

    /**
     * Defaults to the same env as multipart uploads ({@code APP_MULTIPART_MAX_FILE_SIZE}).
     */
    private DataSize maxFileSize = DataSize.ofMegabytes(20);

    private boolean enforceContentTypes = true;

    private List<String> allowedContentTypesLead = new ArrayList<>(List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf"));

    private List<String> allowedContentTypesJob = new ArrayList<>(List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf"));

    /** Receipts: PDFs and images ({@code image/*} prefix supported). */
    private List<String> allowedContentTypesReceipt = new ArrayList<>(List.of(
            "application/pdf",
            "image/*"));

    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Human-readable limit for API error messages ({@code 20MB}, {@code 512KB}, etc.).
     * Prefer whole binary units; otherwise fall back to an exact byte count.
     */
    public String getMaxFileSizeDisplayString() {
        long bytes = maxFileSize.toBytes();
        if (bytes < 0) {
            return maxFileSize.toString();
        }
        long gib = 1024L * 1024L * 1024L;
        long mib = 1024L * 1024L;
        long kib = 1024L;
        if (bytes != 0 && bytes % gib == 0) {
            return (bytes / gib) + "GB";
        }
        if (bytes != 0 && bytes % mib == 0) {
            return (bytes / mib) + "MB";
        }
        if (bytes != 0 && bytes % kib == 0) {
            return (bytes / kib) + "KB";
        }
        return bytes + (bytes == 1L ? " byte" : " bytes");
    }

    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public boolean isEnforceContentTypes() {
        return enforceContentTypes;
    }

    public void setEnforceContentTypes(boolean enforceContentTypes) {
        this.enforceContentTypes = enforceContentTypes;
    }

    public List<String> getAllowedContentTypesLead() {
        return allowedContentTypesLead;
    }

    public void setAllowedContentTypesLead(List<String> allowedContentTypesLead) {
        this.allowedContentTypesLead = allowedContentTypesLead != null ? allowedContentTypesLead : new ArrayList<>();
    }

    public List<String> getAllowedContentTypesJob() {
        return allowedContentTypesJob;
    }

    public void setAllowedContentTypesJob(List<String> allowedContentTypesJob) {
        this.allowedContentTypesJob = allowedContentTypesJob != null ? allowedContentTypesJob : new ArrayList<>();
    }

    public List<String> getAllowedContentTypesReceipt() {
        return allowedContentTypesReceipt;
    }

    public void setAllowedContentTypesReceipt(List<String> allowedContentTypesReceipt) {
        this.allowedContentTypesReceipt =
                allowedContentTypesReceipt != null ? allowedContentTypesReceipt : new ArrayList<>();
    }
}
