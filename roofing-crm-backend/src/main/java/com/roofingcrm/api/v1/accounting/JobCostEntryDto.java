package com.roofingcrm.api.v1.accounting;

import com.roofingcrm.domain.enums.JobCostCategory;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class JobCostEntryDto {

    private UUID id;
    private UUID jobId;
    private JobCostCategory category;
    private String vendorName;
    private String description;
    private BigDecimal amount;
    private Instant incurredAt;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
