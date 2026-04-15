package com.roofingcrm.api.v1.customerreport;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CustomerPhotoReportSectionPhotoDto {

    private UUID attachmentId;
    private int sortOrder;
}
