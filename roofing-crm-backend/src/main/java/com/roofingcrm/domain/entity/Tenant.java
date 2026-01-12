package com.roofingcrm.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a tenant (company/organization) in the multi-tenant system.
 * This is the root entity for multi-tenancy - all other business entities
 * belong to a tenant.
 */
@Entity
@Table(name = "tenants",
        indexes = {
                @Index(name = "idx_tenant_slug", columnList = "slug")
        })
@Getter
@Setter
@NoArgsConstructor
public class Tenant extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @Column(unique = true)
    private String slug;
}
