package com.roofingcrm.api.v1.accounting;

import com.roofingcrm.domain.enums.JobCostCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class CreateJobCostEntryRequest {

    @NotNull(message = "category is required")
    private JobCostCategory category;

    private String vendorName;

    @NotBlank(message = "description is required")
    private String description;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "amount must be greater than or equal to 0")
    private BigDecimal amount;

    @NotNull(message = "incurredAt is required")
    private Instant incurredAt;

    private String notes;
}
