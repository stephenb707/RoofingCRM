package com.roofingcrm.domain.entity;

import com.roofingcrm.domain.enums.AttachmentTag;
import com.roofingcrm.domain.enums.JobCostCategory;
import com.roofingcrm.domain.enums.ReceiptAmountConfidence;
import com.roofingcrm.domain.enums.ReceiptFieldConfidence;
import com.roofingcrm.domain.enums.ReceiptExtractionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a file attachment associated with leads or jobs.
 * Supports both local storage and external providers like S3.
 */
@Entity
@Table(
    name = "attachments",
    indexes = {
        @Index(name = "idx_attachment_tenant_lead", columnList = "tenant_id, lead_id"),
        @Index(name = "idx_attachment_tenant_job", columnList = "tenant_id, job_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Attachment extends TenantAuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_cost_entry_id")
    private JobCostEntry jobCostEntry;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 50)
    private String storageProvider;   // e.g. "LOCAL", "S3"

    private String storageKey;        // path or external key

    // Optional link to an external provider (e.g., CompanyCam)
    private String externalProvider;   // e.g. "COMPANYCAM"
    private String externalAssetId;    // asset ID in external system

    private String description;        // optional description/label

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AttachmentTag tag = AttachmentTag.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReceiptExtractionStatus extractionStatus = ReceiptExtractionStatus.NOT_STARTED;

    private Instant extractedAt;

    @Column(length = 255)
    private String extractionError;

    @Column(length = 255)
    private String extractedVendorName;

    private Instant extractedIncurredAt;

    @Column(precision = 12, scale = 2)
    private BigDecimal extractedAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal extractedSubtotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal extractedTax;

    @Column(precision = 12, scale = 2)
    private BigDecimal extractedTotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal extractedAmountPaid;

    @Column(precision = 12, scale = 2)
    private BigDecimal computedTotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal summaryRegionSubtotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal summaryRegionTax;

    @Column(precision = 12, scale = 2)
    private BigDecimal summaryRegionTotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal summaryRegionAmountPaid;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private JobCostCategory extractedSuggestedCategory;

    @Column(columnDefinition = "text")
    private String extractedNotes;

    private Integer extractionConfidence;

    @Column(columnDefinition = "text")
    private String extractedAmountCandidatesJson;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ReceiptAmountConfidence extractedAmountConfidence;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ReceiptFieldConfidence subtotalConfidence;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ReceiptFieldConfidence taxConfidence;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ReceiptFieldConfidence totalConfidence;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ReceiptFieldConfidence amountPaidConfidence;

    /** Parsed tax rate from receipt summary (e.g. 7.5%), supporting evidence only. */
    @Column(precision = 6, scale = 3)
    private BigDecimal extractedTaxRatePercent;

    @Column(columnDefinition = "text")
    private String extractedWarningsJson;

    @Column(columnDefinition = "text")
    private String extractedRawText;

    @Column(columnDefinition = "text")
    private String summaryRegionRawText;
}
