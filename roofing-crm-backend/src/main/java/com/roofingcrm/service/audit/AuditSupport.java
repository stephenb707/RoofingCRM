package com.roofingcrm.service.audit;

import com.roofingcrm.domain.entity.TenantAuditedEntity;

import java.util.UUID;

public final class AuditSupport {

    private AuditSupport() {
    }

    public static void touchForCreate(TenantAuditedEntity entity, UUID userId) {
        entity.setCreatedByUserId(userId);
        entity.setUpdatedByUserId(userId);
    }

    public static void touchForUpdate(TenantAuditedEntity entity, UUID userId) {
        entity.setUpdatedByUserId(userId);
    }
}
