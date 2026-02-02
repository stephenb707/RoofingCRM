package com.roofingcrm.api.v1.lead;

import com.roofingcrm.domain.enums.LeadStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLeadStatusRequest {

    @NotNull
    private LeadStatus status;

    /** Optional position for reordering (0-based index in destination column). */
    @Min(0)
    private Integer position;
}
