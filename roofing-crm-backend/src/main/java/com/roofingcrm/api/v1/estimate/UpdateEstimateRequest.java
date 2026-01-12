package com.roofingcrm.api.v1.estimate;

import com.roofingcrm.domain.enums.EstimateStatus;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UpdateEstimateRequest {

    private String title;
    private String notes;

    private LocalDate issueDate;
    private LocalDate validUntil;

    @Valid
    private List<EstimateItemRequest> items;

    private EstimateStatus status;
}
