package com.roofingcrm.api.v1.estimate;

import com.roofingcrm.domain.enums.EstimateStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CreateEstimateRequest {

    private String title;
    private String notes;

    private LocalDate issueDate;
    private LocalDate validUntil;

    @Valid
    @NotEmpty
    private List<EstimateItemRequest> items;

    // Optional initial status; if null, default to DRAFT
    private EstimateStatus status;
}
