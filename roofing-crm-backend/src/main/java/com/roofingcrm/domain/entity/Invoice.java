package com.roofingcrm.domain.entity;

import com.roofingcrm.domain.enums.InvoiceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an invoice for a job, created from an accepted estimate.
 */
@Entity
@Table(name = "invoices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_invoices_tenant_number", columnNames = {"tenant_id", "invoice_number"})
        },
        indexes = {
                @Index(name = "idx_invoices_tenant_job", columnList = "tenant_id, job_id"),
                @Index(name = "idx_invoices_tenant_status", columnList = "tenant_id, status")
        })
@Getter
@Setter
@NoArgsConstructor
public class Invoice extends TenantAuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimate_id")
    private Estimate estimate;

    @NotBlank
    @Column(name = "invoice_number", nullable = false, length = 32)
    private String invoiceNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InvoiceStatus status;

    @NotNull
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @NotNull
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal total;

    @Column(columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }
}
