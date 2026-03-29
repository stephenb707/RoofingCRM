package com.roofingcrm.api.v1.accounting;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class JobReceiptDto {

    private UUID id;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String description;
    private Instant uploadedAt;
    private UUID linkedCostEntryId;
    private String linkedCostEntryDescription;
    private BigDecimal linkedCostEntryAmount;
}
