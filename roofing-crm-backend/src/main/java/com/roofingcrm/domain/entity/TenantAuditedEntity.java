package com.roofingcrm.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Base entity for all tenant-scoped entities.
 * Extends BaseEntity and adds tenant reference for multi-tenancy support.
 * Also includes lightweight audit fields for tracking who created/updated records.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class TenantAuditedEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "updated_by_user_id")
    private UUID updatedByUserId;

    protected TenantAuditedEntity() {
        super();
    }
}
