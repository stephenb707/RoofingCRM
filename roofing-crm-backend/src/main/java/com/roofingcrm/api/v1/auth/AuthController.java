package com.roofingcrm.api.v1.auth;

import com.roofingcrm.security.RefreshTokenProperties;
import com.roofingcrm.service.auth.AuthService;
import com.roofingcrm.service.auth.AuthSessionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenProperties refreshTokenProperties;

    public AuthController(AuthService authService, RefreshTokenProperties refreshTokenProperties) {
        this.authService = authService;
        this.refreshTokenProperties = refreshTokenProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.registerOwner(request);
        return withRefreshCookie(ResponseEntity.status(HttpStatus.CREATED), response).body(response);
    }

    @PostMapping("/register-with-invite")
    public ResponseEntity<AuthResponse> registerWithInvite(@Valid @RequestBody RegisterWithInviteRequest request) {
        AuthResponse response = authService.registerWithInvite(request);
        return withRefreshCookie(ResponseEntity.status(HttpStatus.CREATED), response).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return withRefreshCookie(ResponseEntity.ok(), response).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "${app.security.refresh-token.cookie-name:rc_refresh_token}", required = false)
            String refreshToken,
            HttpServletRequest request) {
        String csrfHeader = readCsrfHeader(request);
        try {
            AuthResponse response = authService.refresh(
                    refreshToken,
                    csrfHeader,
                    request.getHeader(HttpHeaders.USER_AGENT),
                    clientIp(request));
            return withRefreshCookie(ResponseEntity.ok(), response).body(response);
        } catch (AuthSessionException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                    .build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${app.security.refresh-token.cookie-name:rc_refresh_token}", required = false)
            String refreshToken,
            HttpServletRequest request) {
        String csrfHeader = readCsrfHeader(request);
        try {
            authService.logout(refreshToken, csrfHeader);
        } catch (AuthSessionException ex) {
            // Logout always clears the cookie client-side; we still 401 so the SPA learns the
            // CSRF token was wrong (e.g. dev forgot to send it) without leaving stale state.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                    .build();
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    private ResponseEntity.BodyBuilder withRefreshCookie(ResponseEntity.BodyBuilder builder, AuthResponse response) {
        if (response.getRefreshToken() == null || response.getRefreshToken().isBlank()) {
            return builder;
        }
        return builder.header(HttpHeaders.SET_COOKIE, refreshCookie(response.getRefreshToken()).toString());
    }

    private ResponseCookie refreshCookie(String token) {
        return ResponseCookie.from(Objects.requireNonNull(refreshTokenProperties.getCookieName()), Objects.requireNonNull(token))
                .httpOnly(true)
                .secure(refreshTokenProperties.isSecureCookie())
                .sameSite(refreshTokenProperties.getSameSite())
                .path(refreshTokenProperties.getCookiePath())
                .maxAge(Objects.requireNonNull(Duration.ofDays(refreshTokenProperties.getExpirationDays())))
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(Objects.requireNonNull(refreshTokenProperties.getCookieName()), "")
                .httpOnly(true)
                .secure(refreshTokenProperties.isSecureCookie())
                .sameSite(refreshTokenProperties.getSameSite())
                .path(refreshTokenProperties.getCookiePath())
                .maxAge(Objects.requireNonNull(Duration.ZERO))
                .build();
    }

    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String readCsrfHeader(HttpServletRequest request) {
        String value = request.getHeader("X-CSRF-Refresh");
        return value != null ? value.trim() : null;
    }
}
