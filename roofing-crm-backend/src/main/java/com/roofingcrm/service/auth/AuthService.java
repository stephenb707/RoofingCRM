package com.roofingcrm.service.auth;

import com.roofingcrm.api.v1.auth.AuthResponse;
import com.roofingcrm.api.v1.auth.LoginRequest;
import com.roofingcrm.api.v1.auth.RegisterRequest;
import com.roofingcrm.api.v1.auth.RegisterWithInviteRequest;

public interface AuthService {

    AuthResponse registerOwner(RegisterRequest request);

    AuthResponse registerWithInvite(RegisterWithInviteRequest request);

    AuthResponse login(LoginRequest request);

    /**
     * Rotates a refresh-token session and returns a fresh access token + new refresh/CSRF tokens.
     *
     * @param refreshToken plaintext value from the HttpOnly refresh cookie
     * @param csrfHeaderValue plaintext value sent in the X-CSRF-Refresh header
     */
    AuthResponse refresh(String refreshToken, String csrfHeaderValue, String userAgent, String ipAddress);

    /**
     * Revokes the refresh-token session. Requires a matching CSRF header so a third-party
     * cannot log a user out by triggering a request with their refresh cookie.
     */
    void logout(String refreshToken, String csrfHeaderValue);
}
