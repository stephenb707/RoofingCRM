package com.roofingcrm.api.v1.estimate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShareEstimateRequest {

    @Min(1)
    @Max(365)
    private Integer expiresInDays = 14;
}
