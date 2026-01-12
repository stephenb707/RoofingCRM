package com.roofingcrm.service.tenant;

import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.repository.TenantUserMembershipRepository;
import com.roofingcrm.domain.repository.UserRepository;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Check membership exists - throws if not found
        membershipRepository.findByTenantAndUser(tenant, user)
                .orElseThrow(() -> new TenantAccessDeniedException("User does not have access to this tenant"));

        // For now we don't enforce specific roles here; that will come in a later step.
        if (!user.isEnabled()) {
            throw new TenantAccessDeniedException("User account is disabled");
        }

        return tenant;
    }
}
