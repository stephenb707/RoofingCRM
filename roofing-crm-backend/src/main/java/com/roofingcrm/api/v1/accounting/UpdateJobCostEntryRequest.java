package com.roofingcrm.api.v1.accounting;

import com.roofingcrm.domain.enums.JobCostCategory;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class UpdateJobCostEntryRequest {

    private JobCostCategory category;

    private String vendorName;

    private String description;

    @DecimalMin(value = "0.00", inclusive = true, message = "amount must be greater than or equal to 0")
    private BigDecimal amount;

    private Instant incurredAt;

    private String notes;
}
