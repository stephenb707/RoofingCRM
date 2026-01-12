package com.roofingcrm.api.v1.job;

import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.enums.JobType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class JobDto {

    private UUID id;

    private UUID customerId;
    private UUID leadId;

    private JobStatus status;
    private JobType type;

    private AddressDto propertyAddress;

    private LocalDate scheduledStartDate;
    private LocalDate scheduledEndDate;

    private String internalNotes;
    private String crewName;

    private Instant createdAt;
    private Instant updatedAt;
}
