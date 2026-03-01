package com.roofingcrm.api.v1.estimate;

import com.roofingcrm.domain.enums.EstimateStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class EstimateSummaryDto {

    private UUID id;
    private UUID jobId;
    private UUID customerId;
    private EstimateStatus status;
    private String title;
    private String notes;
    private LocalDate issueDate;
    private LocalDate validUntil;
    private BigDecimal subtotal;
    private BigDecimal total;
    private Instant createdAt;
    private Instant updatedAt;
}
