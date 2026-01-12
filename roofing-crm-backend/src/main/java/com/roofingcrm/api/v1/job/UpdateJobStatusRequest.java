package com.roofingcrm.api.v1.job;

import com.roofingcrm.domain.enums.JobStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateJobStatusRequest {

    @NotNull
    private JobStatus status;
}
