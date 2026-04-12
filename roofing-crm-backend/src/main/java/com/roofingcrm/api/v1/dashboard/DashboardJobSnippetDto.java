package com.roofingcrm.api.v1.dashboard;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class DashboardJobSnippetDto {

    private UUID id;

    private String statusKey;

    private String statusLabel;

    private LocalDate scheduledStartDate;

    private String propertyLine1;

    private String customerLabel;
}
