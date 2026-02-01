package com.roofingcrm.service.tenant;

import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.enums.UserRole;
import org.springframework.lang.NonNull;

import java.util.Set;
import java.util.UUID;

public interface TenantAccessService {

    /**
     * Ensure that the given user has access to the given tenant.
     * Returns the Tenant if access is allowed, otherwise throws an access exception.
     * Archived memberships do not count as access.
     */
    Tenant loadTenantForUserOrThrow(@NonNull UUID tenantId, @NonNull UUID userId);

    /**
     * Load the active (non-archived) membership for the user in the tenant.
     */
    TenantUserMembership loadMembershipForUserOrThrow(@NonNull UUID tenantId, @NonNull UUID userId);

    /**
     * Ensure the user has one of the allowed roles in the tenant.
     * Throws TenantAccessDeniedException if not.
     */
    TenantUserMembership requireAnyRole(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull Set<UserRole> allowedRoles);
}
