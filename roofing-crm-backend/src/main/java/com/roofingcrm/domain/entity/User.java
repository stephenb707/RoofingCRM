package com.roofingcrm.domain.entity;

import com.roofingcrm.domain.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a user within a tenant.
 * Users have roles that determine their permissions within the system.
 */
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_tenant_email", columnNames = {"tenant_id", "email"})
        },
        indexes = {
                @Index(name = "idx_user_tenant_role", columnList = "tenant_id, role")
        })
@Getter
@Setter
@NoArgsConstructor
public class User extends TenantAuditedEntity {

    @NotBlank
    @Email
    @Column(nullable = false)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    @NotBlank
    @Column(nullable = false)
    private String firstName;

    @NotBlank
    @Column(nullable = false)
    private String lastName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;
}
