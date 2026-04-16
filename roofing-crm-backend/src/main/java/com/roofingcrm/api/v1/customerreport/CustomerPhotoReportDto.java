package com.roofingcrm.api.v1.customerreport;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CustomerPhotoReportDto {

    private UUID id;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private UUID jobId;
    private String jobDisplayName;
    private String title;
    private String reportType;
    private String summary;
    private List<CustomerPhotoReportSectionDto> sections = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
}
