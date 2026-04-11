package com.roofingcrm.api.v1.dashboard;

import com.roofingcrm.domain.enums.JobStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class DashboardJobSnippetDto {

    private UUID id;

    private JobStatus status;

    private LocalDate scheduledStartDate;

    private String propertyLine1;

    private String customerLabel;
}
