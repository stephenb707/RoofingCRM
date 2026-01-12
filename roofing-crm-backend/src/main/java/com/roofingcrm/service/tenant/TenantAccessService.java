package com.roofingcrm.service.tenant;

import com.roofingcrm.domain.entity.Tenant;
import org.springframework.lang.NonNull;

import java.util.UUID;

public interface TenantAccessService {

    /**
     * Ensure that the given user has access to the given tenant.
     * Returns the Tenant if access is allowed, otherwise throws an access exception.
     */
    Tenant loadTenantForUserOrThrow(@NonNull UUID tenantId, @NonNull UUID userId);
}
