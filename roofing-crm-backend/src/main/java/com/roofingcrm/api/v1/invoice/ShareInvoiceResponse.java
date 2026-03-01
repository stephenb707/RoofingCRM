package com.roofingcrm.api.v1.invoice;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ShareInvoiceResponse {

    private String token;
    private Instant expiresAt;
}
