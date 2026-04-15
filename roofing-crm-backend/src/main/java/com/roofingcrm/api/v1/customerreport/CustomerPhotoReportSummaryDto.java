package com.roofingcrm.api.v1.customerreport;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class CustomerPhotoReportSummaryDto {

    private UUID id;
    private String title;
    private String reportType;
    private UUID customerId;
    private String customerName;
    private UUID jobId;
    private String jobDisplayName;
    private Instant updatedAt;
}
