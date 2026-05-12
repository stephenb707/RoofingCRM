package com.roofingcrm.api.v1.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AuthResponse {

    private String token;

    @JsonIgnore
    private String refreshToken;

    /**
     * JS-readable CSRF token bound to the current refresh session. The frontend stores this and
     * sends it in the X-CSRF-Refresh header for refresh/logout calls. The refresh cookie itself
     * is HttpOnly, so requiring this paired token prevents CSRF on the refresh endpoint.
     */
    private String csrfToken;

    private UUID userId;
    private String email;
    private String fullName;

    private List<TenantSummaryDto> tenants;
}
