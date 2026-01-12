package com.roofingcrm.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Represents a file attachment associated with various parent entities
 * (leads, jobs, estimates, etc.). Supports both internal storage and
 * external providers like CompanyCam.
 */
@Entity
@Table(
    name = "attachments",
    indexes = {
        @Index(name = "idx_attachment_tenant_parent", columnList = "tenant_id, parentType, parentId")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Attachment extends TenantAuditedEntity {

    @Column(nullable = false, length = 50)
    private String parentType; // e.g. "LEAD", "JOB", "ESTIMATE"

    @Column(nullable = false)
    private UUID parentId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private String storageUrl; // e.g. S3 URL or GCS URL

    // Optional link to an external provider (e.g., CompanyCam)
    private String externalProvider;   // e.g. "COMPANYCAM"
    private String externalAssetId;    // asset ID in external system
}
