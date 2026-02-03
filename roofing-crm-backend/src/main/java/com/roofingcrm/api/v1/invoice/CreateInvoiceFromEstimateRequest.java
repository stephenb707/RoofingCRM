package com.roofingcrm.api.v1.invoice;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class CreateInvoiceFromEstimateRequest {

    @NotNull(message = "estimateId is required")
    private UUID estimateId;

    private Instant dueAt;
    private String notes;
}
