package com.roofingcrm.api.v1.customerreport;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CustomerPhotoReportSectionRequest {

    private String title;
    private String body;
    private List<UUID> attachmentIds = new ArrayList<>();
}
