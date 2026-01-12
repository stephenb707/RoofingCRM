package com.roofingcrm.api.v1.estimate;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class EstimateItemDto {

    private UUID id;

    private String name;
    private String description;

    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private String unit;
}
