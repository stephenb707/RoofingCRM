package com.roofingcrm.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Base entity for all tenant-scoped entities.
 * Extends BaseEntity and adds tenant reference for multi-tenancy support.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class TenantAuditedEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    protected TenantAuditedEntity() {
        super();
    }
}
