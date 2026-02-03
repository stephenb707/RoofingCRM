package com.roofingcrm.api.v1.invoice;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class InvoiceItemDto {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private Integer sortOrder;
}
