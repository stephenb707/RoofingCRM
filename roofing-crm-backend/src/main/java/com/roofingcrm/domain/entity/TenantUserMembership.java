package com.roofingcrm.domain.entity;

import com.roofingcrm.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a user's membership in a tenant with a specific role.
 * A user can have different roles in different tenants.
 */
@Entity
@Table(
    name = "tenant_user_memberships",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_user", columnNames = {"tenant_id", "user_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class TenantUserMembership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;
}
