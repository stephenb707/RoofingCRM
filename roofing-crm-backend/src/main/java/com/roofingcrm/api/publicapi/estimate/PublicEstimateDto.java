package com.roofingcrm.api.publicapi.estimate;

import com.roofingcrm.domain.enums.EstimateStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class PublicEstimateDto {

    private String estimateNumber;
    private EstimateStatus status;
    private String title;
    private String notes;
    private LocalDate issueDate;
    private LocalDate validUntil;
    private BigDecimal subtotal;
    private BigDecimal total;
    private Instant publicExpiresAt;

    private String customerName;
    private String customerAddress;

    private List<PublicEstimateItemDto> items;
}
