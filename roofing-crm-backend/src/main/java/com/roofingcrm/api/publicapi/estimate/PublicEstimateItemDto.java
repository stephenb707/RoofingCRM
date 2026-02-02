package com.roofingcrm.api.publicapi.estimate;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PublicEstimateItemDto {

    private String name;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private String unit;
    private BigDecimal lineTotal;
}
