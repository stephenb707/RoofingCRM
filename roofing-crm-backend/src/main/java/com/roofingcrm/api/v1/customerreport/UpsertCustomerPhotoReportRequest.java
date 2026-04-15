package com.roofingcrm.api.v1.customerreport;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UpsertCustomerPhotoReportRequest {

    @NotNull
    private UUID customerId;

    private UUID jobId;

    @NotBlank
    private String title;

    private String reportType;

    private String summary;

    @Valid
    private List<CustomerPhotoReportSectionRequest> sections = new ArrayList<>();
}
