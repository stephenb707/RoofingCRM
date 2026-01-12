package com.roofingcrm.api.v1.lead;

import com.roofingcrm.domain.enums.LeadStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLeadStatusRequest {

    @NotNull
    private LeadStatus status;
}
