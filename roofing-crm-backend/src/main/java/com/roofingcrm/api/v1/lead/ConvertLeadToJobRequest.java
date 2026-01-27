package com.roofingcrm.api.v1.lead;

import com.roofingcrm.domain.enums.JobType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ConvertLeadToJobRequest {

    @NotNull
    private JobType type;

    private LocalDate scheduledStartDate;

    private LocalDate scheduledEndDate;

    private String crewName;

    private String internalNotes;
}
