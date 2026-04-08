package com.roofingcrm.api.v1.accounting;

import com.roofingcrm.domain.enums.ReceiptExtractionStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ExtractReceiptResponseDto {

    private UUID receiptId;
    private ReceiptExtractionStatus status;
    private Instant extractedAt;
    private String error;
    private Integer confidence;
    private ReceiptExtractionResultDto result;
}
