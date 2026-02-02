package com.roofingcrm.service.tenant;

import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.repository.TenantUserMembershipRepository;
import com.roofingcrm.domain.repository.UserRepository;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TenantAccessServiceImpl implements TenantAccessService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantUserMembershipRepository membershipRepository;

    public TenantAccessServiceImpl(TenantRepository tenantRepository,
                                   UserRepository userRepository,
                                   TenantUserMembershipRepository membershipRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    public Tenant loadTenantForUserOrThrow(@NonNull UUID tenantId, @NonNull UUID userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check active (non-archived) membership exists - throws if not found
        membershipRepository.findByTenantAndUserAndArchivedFalse(tenant, user)
                .orElseThrow(() -> new TenantAccessDeniedException("User does not have access to this tenant"));

        if (!user.isEnabled()) {
            throw new TenantAccessDeniedException("User account is disabled");
        }

        return tenant;
    }

    @Override
    public TenantUserMembership loadMembershipForUserOrThrow(@NonNull UUID tenantId, @NonNull UUID userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return membershipRepository.findByTenantAndUserAndArchivedFalse(tenant, user)
                .orElseThrow(() -> new TenantAccessDeniedException("User does not have access to this tenant"));
    }

    @Override
    public TenantUserMembership requireAnyRole(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull Set<UserRole> allowedRoles) {
        return requireAnyRole(tenantId, userId, allowedRoles, "You do not have permission to perform this action.");
    }

    @Override
    public TenantUserMembership requireAnyRole(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull Set<UserRole> allowedRoles, @NonNull String deniedMessage) {
        TenantUserMembership membership = loadMembershipForUserOrThrow(tenantId, userId);
        if (!allowedRoles.contains(membership.getRole())) {
            throw new TenantAccessDeniedException(deniedMessage);
        }
        return membership;
    }
}
