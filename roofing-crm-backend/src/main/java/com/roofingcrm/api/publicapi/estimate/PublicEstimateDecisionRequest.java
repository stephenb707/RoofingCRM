package com.roofingcrm.api.publicapi.estimate;

import com.roofingcrm.domain.enums.EstimateStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicEstimateDecisionRequest {

    @NotNull(message = "decision is required")
    private EstimateStatus decision; // ACCEPTED or REJECTED

    @NotBlank(message = "signerName is required")
    private String signerName;

    @Email
    private String signerEmail;
}
