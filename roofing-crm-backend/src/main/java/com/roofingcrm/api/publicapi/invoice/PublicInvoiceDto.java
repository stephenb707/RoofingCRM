package com.roofingcrm.api.publicapi.invoice;

import com.roofingcrm.domain.enums.InvoiceStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class PublicInvoiceDto {

    private String invoiceNumber;
    private InvoiceStatus status;
    private Instant issuedAt;
    private Instant dueAt;
    private Instant sentAt;
    private BigDecimal total;
    private String notes;
    private Instant publicExpiresAt;

    private String customerName;
    private String customerAddress;

    private List<PublicInvoiceItemDto> items;
}
