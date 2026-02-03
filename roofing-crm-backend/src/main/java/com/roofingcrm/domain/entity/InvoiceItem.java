package com.roofingcrm.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Snapshot line item copied from an estimate when creating an invoice.
 */
@Entity
@Table(name = "invoice_items",
        indexes = {
                @Index(name = "idx_invoice_items_invoice_sort", columnList = "invoice_id, sort_order")
        })
@Getter
@Setter
@NoArgsConstructor
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
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

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
