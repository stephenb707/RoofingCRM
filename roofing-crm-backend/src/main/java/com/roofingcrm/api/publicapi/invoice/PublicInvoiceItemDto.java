package com.roofingcrm.api.publicapi.invoice;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PublicInvoiceItemDto {

    private String name;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private String unit;
    private BigDecimal lineTotal;
}
