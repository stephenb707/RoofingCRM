package com.roofingcrm.api.v1.invoice;

import com.roofingcrm.domain.enums.InvoiceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateInvoiceStatusRequest {

    @NotNull(message = "status is required")
    private InvoiceStatus status;
}
