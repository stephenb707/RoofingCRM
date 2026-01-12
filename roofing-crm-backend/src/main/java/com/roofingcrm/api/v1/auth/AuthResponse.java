package com.roofingcrm.api.v1.auth;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AuthResponse {

    private String token;

    private UUID userId;
    private String email;
    private String fullName;

    private List<TenantSummaryDto> tenants;
}
