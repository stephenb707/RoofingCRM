package com.roofingcrm.api.v1.invoice;

import com.roofingcrm.domain.enums.InvoiceStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class InvoiceDto {

    private UUID id;
    private String invoiceNumber;
    private InvoiceStatus status;
    private Instant issuedAt;
    private Instant sentAt;
    private Instant dueAt;
    private Instant paidAt;
    private BigDecimal total;
    private String notes;
    private UUID jobId;
    private UUID estimateId;
    private List<InvoiceItemDto> items;
    private Instant createdAt;
    private Instant updatedAt;
}
