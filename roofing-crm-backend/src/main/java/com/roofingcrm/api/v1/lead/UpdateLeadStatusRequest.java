package com.roofingcrm.api.v1.lead;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateLeadStatusRequest {

    @NotNull
    private UUID statusDefinitionId;

    /** Optional position for reordering (0-based index in destination column). */
    @Min(0)
    private Integer position;
}
