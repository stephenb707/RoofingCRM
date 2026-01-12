package com.roofingcrm.api.v1.estimate;

import com.roofingcrm.domain.enums.EstimateStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEstimateStatusRequest {

    @NotNull
    private EstimateStatus status;
}
