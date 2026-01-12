package com.roofingcrm.api.v1.job;

import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.domain.enums.JobType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateJobRequest {

    // Option 1: from an existing lead (preferred pipeline flow)
    private UUID leadId;

    // Option 2: directly from a customer (no lead), required if leadId is null
    private UUID customerId;

    @NotNull
    private JobType type;

    @Valid
    @NotNull
    private AddressDto propertyAddress;

    private LocalDate scheduledStartDate;
    private LocalDate scheduledEndDate;

    private String internalNotes;
    private String crewName;
}
