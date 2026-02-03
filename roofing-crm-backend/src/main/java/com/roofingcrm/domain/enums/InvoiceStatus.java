package com.roofingcrm.domain.enums;

/**
 * Status of an invoice.
 */
public enum InvoiceStatus {
    DRAFT,
    SENT,
    PAID,
    VOID;

    public boolean isTerminal() {
        return this == PAID || this == VOID;
    }
}
