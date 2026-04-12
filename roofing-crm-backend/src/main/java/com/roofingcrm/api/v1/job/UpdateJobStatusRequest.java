package com.roofingcrm.api.v1.job;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateJobStatusRequest {

    @NotNull
    private UUID statusDefinitionId;
}
