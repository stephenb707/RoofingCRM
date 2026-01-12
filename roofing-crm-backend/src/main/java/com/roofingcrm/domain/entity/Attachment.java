package com.roofingcrm.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
