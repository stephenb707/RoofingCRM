package com.roofingcrm.domain.entity;

import com.roofingcrm.domain.enums.JobCostCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "job_cost_entries",
        indexes = {
                @Index(name = "idx_job_cost_entries_tenant_job", columnList = "tenant_id, job_id"),
                @Index(name = "idx_job_cost_entries_category_incurred_at", columnList = "category, incurred_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class JobCostEntry extends TenantAuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobCostCategory category;

    @Column(name = "vendor_name", length = 255)
    private String vendorName;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String description;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "incurred_at", nullable = false)
    private Instant incurredAt;

    @Column(columnDefinition = "text")
    private String notes;
}
