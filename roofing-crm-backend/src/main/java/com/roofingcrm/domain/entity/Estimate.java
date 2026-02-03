package com.roofingcrm.domain.entity;

import com.roofingcrm.domain.enums.EstimateStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Represents an estimate/quote for a roofing job.
 * Estimates contain line items and financial totals.
 */
@Entity
@Table(name = "estimates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_estimate_tenant_number", columnNames = {"tenant_id", "estimate_number"})
        },
        indexes = {
                @Index(name = "idx_estimate_tenant_job", columnList = "tenant_id, job_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Estimate extends TenantAuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @NotBlank
    @Column(name = "estimate_number", nullable = false)
    private String estimateNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstimateStatus status;

    private String title;

    private LocalDate issueDate;

    private LocalDate validUntil;

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal tax;

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

    @Column(columnDefinition = "text")
    private String notesForCustomer;

    @Column(columnDefinition = "text")
    private String internalNotes;

    @OneToMany(mappedBy = "estimate", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<EstimateItem> items = new java.util.ArrayList<>();

    @Column(name = "public_token", length = 64)
    private String publicToken;

    @Column(name = "public_enabled", nullable = false)
    private boolean publicEnabled = false;

    @Column(name = "public_expires_at")
    private Instant publicExpiresAt;

    @Column(name = "public_last_shared_at")
    private Instant publicLastSharedAt;

    @Column(name = "decision_at")
    private Instant decisionAt;

    @Column(name = "decision_by_name", length = 255)
    private String decisionByName;

    @Column(name = "decision_by_email", length = 255)
    private String decisionByEmail;
}
