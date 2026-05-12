package com.roofingcrm.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Server-side refresh-token session. The plaintext token is only sent as an HttpOnly cookie;
 * we store a SHA-256 hash because the token itself is high entropy and random.
 */
@Entity
@Table(name = "auth_refresh_token_sessions",
        indexes = {
                @Index(name = "idx_refresh_token_hash", columnList = "token_hash", unique = true),
                @Index(name = "idx_refresh_user_active", columnList = "user_id, revoked_at, expires_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class AuthRefreshTokenSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_id")
    private UUID replacedById;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /**
     * SHA-256 hex digest of the per-session CSRF token. The plaintext CSRF token is JS-readable
     * (returned in the auth response body); the refresh cookie is HttpOnly, so requiring the
     * matching CSRF header prevents cross-site forgery of refresh/logout calls.
     */
    @Column(name = "csrf_token_hash", length = 64)
    private String csrfTokenHash;

    public boolean isUsable(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
