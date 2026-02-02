package com.roofingcrm.api.v1.estimate;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ShareEstimateResponse {

    private String token;
    private Instant expiresAt;
}
