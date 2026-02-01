package com.roofingcrm.api.v1.job;

import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.domain.enums.JobType;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateJobRequest {

    private JobType type;

    @Valid
    private AddressDto propertyAddress;

    private LocalDate scheduledStartDate;
    private LocalDate scheduledEndDate;
    /** When true, clears scheduled dates. */
    private Boolean clearSchedule;

    private String internalNotes;
    private String crewName;
}
