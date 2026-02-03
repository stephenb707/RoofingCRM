package com.roofingcrm.domain.entity;

import com.roofingcrm.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a pending invitation for a user to join a tenant.
 */
@Entity
@Table(
    name = "tenant_invites",
    indexes = {
        @Index(name = "idx_tenant_invites_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_tenant_invites_tenant_email", columnList = "tenant_id, email"),
        @Index(name = "idx_tenant_invites_tenant_accepted", columnList = "tenant_id, accepted_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class TenantInvite {

    @Id
    @Column(name = "invite_id")
    private UUID inviteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Column(nullable = false, unique = true)
    private UUID token;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant acceptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_user_id")
    private User acceptedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(nullable = false)
    private Instant createdAt;
}
