package com.roofingcrm.api.v1.invoice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ShareInvoiceResponse {

    /**
     * Plaintext token for the public URL. Returned on every share so the client can build a link.
     * The database stores only a hash of this value.
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String token;

    private Instant expiresAt;
}
