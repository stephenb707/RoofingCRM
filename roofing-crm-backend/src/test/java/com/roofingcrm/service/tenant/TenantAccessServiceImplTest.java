package com.roofingcrm.service.tenant;

import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.repository.TenantUserMembershipRepository;
import com.roofingcrm.domain.repository.UserRepository;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class TenantAccessServiceImplTest {

    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final TenantUserMembershipRepository membershipRepository =
            mock(TenantUserMembershipRepository.class);

    private TenantAccessServiceImpl tenantAccessService;
    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantAccessService = new TenantAccessServiceImpl(
                tenantRepository, userRepository, membershipRepository);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
    }

    @Test
    void loadTenantReturnsSameErrorForMissingAndNonMemberTenant() {
        ResourceNotFoundException missingTenant = assertThrows(
                ResourceNotFoundException.class,
                () -> tenantAccessService.loadTenantForUserOrThrow(tenantId, userId));

        configureExistingTenantWithoutMembership();
        ResourceNotFoundException nonMemberTenant = assertThrows(
                ResourceNotFoundException.class,
                () -> tenantAccessService.loadTenantForUserOrThrow(tenantId, userId));

        assertEquals(missingTenant.getMessage(), nonMemberTenant.getMessage());
        assertEquals("Tenant not found", nonMemberTenant.getMessage());
    }

    @Test
    void loadMembershipReturnsSameErrorForMissingAndNonMemberTenant() {
        ResourceNotFoundException missingTenant = assertThrows(
                ResourceNotFoundException.class,
                () -> tenantAccessService.loadMembershipForUserOrThrow(tenantId, userId));

        configureExistingTenantWithoutMembership();
        ResourceNotFoundException nonMemberTenant = assertThrows(
                ResourceNotFoundException.class,
                () -> tenantAccessService.loadMembershipForUserOrThrow(tenantId, userId));

        assertEquals(missingTenant.getMessage(), nonMemberTenant.getMessage());
        assertEquals("Tenant not found", nonMemberTenant.getMessage());
    }

    private void configureExistingTenantWithoutMembership() {
        Tenant tenant = mock(Tenant.class);
        User user = mock(User.class);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(membershipRepository.findByTenantAndUserAndArchivedFalse(tenant, user))
                .thenReturn(Optional.empty());
    }
}
