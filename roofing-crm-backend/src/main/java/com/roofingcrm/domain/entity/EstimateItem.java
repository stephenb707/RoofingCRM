package com.roofingcrm.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Represents a line item within an estimate.
 * Each item has a description, quantity, unit price, and calculated line total.
 */
@Entity
@Table(name = "estimate_items",
        indexes = {
                @Index(name = "idx_estimate_item_tenant_estimate", columnList = "tenant_id, estimate_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class EstimateItem extends TenantAuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estimate_id", nullable = false)
    private Estimate estimate;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String description;

    @NotNull
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal quantity;

    @NotNull
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @NotNull
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal lineTotal;

    private String unit;

    private Integer sortOrder;
}
