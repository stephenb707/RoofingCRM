package com.roofingcrm.service.team;

import com.roofingcrm.api.v1.team.CreateInviteRequest;
import com.roofingcrm.api.v1.team.UpdateMemberRoleRequest;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantInvite;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.TenantInviteRepository;
import com.roofingcrm.domain.repository.TenantUserMembershipRepository;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.repository.UserRepository;
import com.roofingcrm.service.exception.InviteConflictException;
import com.roofingcrm.service.tenant.TenantAccessDeniedException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class TeamServiceImplUnitTest {

    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private TenantInviteRepository inviteRepository;
    @Mock
    private TenantUserMembershipRepository membershipRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;

    private TeamServiceImpl service;

    private Tenant tenant;
    private User actorUser;
    private TenantUserMembership actorMembership;
    private UUID tenantId;
    private UUID actorUserId;

    @BeforeEach
    void setUp() {
        service = new TeamServiceImpl(
                tenantAccessService, inviteRepository, membershipRepository,
                tenantRepository, userRepository);

        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("Test Tenant");
        tenantId = tenant.getId();

        actorUser = new User();
        actorUser.setId(UUID.randomUUID());
        actorUser.setEmail("owner@test.com");
        actorUser.setFullName("Owner User");
        actorUserId = actorUser.getId();

        actorMembership = new TenantUserMembership();
        actorMembership.setTenant(tenant);
        actorMembership.setUser(actorUser);
        actorMembership.setRole(UserRole.OWNER);
    }

    @Test
    void createInvite_normalizesEmailAndSavesInvite() {
        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(actorUserId), any())).thenReturn(actorMembership);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findById(actorUserId)).thenReturn(Optional.of(actorUser));
        when(userRepository.findByEmailIgnoreCase("new@test.com")).thenReturn(Optional.empty());
        when(inviteRepository.findByTenantAndEmailIgnoreCaseAndAcceptedAtIsNull(eq(tenant), eq("new@test.com"))).thenReturn(Optional.empty());

        TenantInvite savedInvite = new TenantInvite();
        when(inviteRepository.save(any(TenantInvite.class))).thenAnswer(inv -> {
            TenantInvite inv2 = inv.getArgument(0);
            savedInvite.setInviteId(inv2.getInviteId());
            savedInvite.setTenant(inv2.getTenant());
            savedInvite.setEmail(inv2.getEmail());
            savedInvite.setRole(inv2.getRole());
            savedInvite.setToken(inv2.getToken());
            savedInvite.setExpiresAt(inv2.getExpiresAt());
            savedInvite.setCreatedBy(inv2.getCreatedBy());
            savedInvite.setCreatedAt(inv2.getCreatedAt());
            return inv2;
        });

        CreateInviteRequest req = new CreateInviteRequest();
        req.setEmail("  NEW@TEST.COM  ");
        req.setRole(UserRole.SALES);

        var dto = service.createInvite(tenantId, actorUserId, req);

        assertNotNull(dto.getInviteId());
        assertEquals("new@test.com", dto.getEmail());
        assertEquals(UserRole.SALES, dto.getRole());
        assertNotNull(dto.getToken());

        ArgumentCaptor<TenantInvite> captor = ArgumentCaptor.forClass(TenantInvite.class);
        verify(inviteRepository).save(captor.capture());
        assertEquals("new@test.com", captor.getValue().getEmail());
    }

    @Test
    void createInvite_blocksDuplicatePendingInvite() {
        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(actorUserId), any())).thenReturn(actorMembership);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findById(actorUserId)).thenReturn(Optional.of(actorUser));
        when(userRepository.findByEmailIgnoreCase("dup@test.com")).thenReturn(Optional.empty());

        TenantInvite existingInvite = new TenantInvite();
        existingInvite.setInviteId(UUID.randomUUID());
        existingInvite.setEmail("dup@test.com");
        when(inviteRepository.findByTenantAndEmailIgnoreCaseAndAcceptedAtIsNull(eq(tenant), eq("dup@test.com")))
                .thenReturn(Optional.of(existingInvite));

        CreateInviteRequest req = new CreateInviteRequest();
        req.setEmail("dup@test.com");
        req.setRole(UserRole.ADMIN);

        assertThrows(InviteConflictException.class, () ->
                service.createInvite(tenantId, actorUserId, req));

        verify(inviteRepository, never()).save(any());
    }

    @Test
    void createInvite_blocksIfAlreadyMember() {
        User existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setEmail("member@test.com");

        TenantUserMembership existingMembership = new TenantUserMembership();
        existingMembership.setTenant(tenant);
        existingMembership.setUser(existingUser);

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(actorUserId), any())).thenReturn(actorMembership);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findById(actorUserId)).thenReturn(Optional.of(actorUser));
        when(userRepository.findByEmailIgnoreCase("member@test.com")).thenReturn(Optional.of(existingUser));
        when(membershipRepository.findByTenantAndUserAndArchivedFalse(tenant, existingUser))
                .thenReturn(Optional.of(existingMembership));

        CreateInviteRequest req = new CreateInviteRequest();
        req.setEmail("member@test.com");
        req.setRole(UserRole.SALES);

        assertThrows(InviteConflictException.class, () ->
                service.createInvite(tenantId, actorUserId, req));

        verify(inviteRepository, never()).save(any());
    }

    @Test
    void updateMemberRole_blocksSelfChange() {
        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(actorUserId), any())).thenReturn(actorMembership);

        UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
        req.setRole(UserRole.ADMIN);

        assertThrows(TenantAccessDeniedException.class, () ->
                service.updateMemberRole(tenantId, actorUserId, actorUserId, req));

        verify(membershipRepository, never()).save(any());
    }

    @Test
    void removeMember_blocksRemovingSelf() {
        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(actorUserId), any())).thenReturn(actorMembership);

        assertThrows(TenantAccessDeniedException.class, () ->
                service.removeMember(tenantId, actorUserId, actorUserId));

        verify(membershipRepository, never()).save(any());
    }

    @Test
    void updateMemberRole_blocksDemotingLastOwner() {
        UUID targetUserId = UUID.randomUUID();
        User targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setEmail("other@test.com");

        TenantUserMembership targetMembership = new TenantUserMembership();
        targetMembership.setTenant(tenant);
        targetMembership.setUser(targetUser);
        targetMembership.setRole(UserRole.OWNER);

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(actorUserId), any())).thenReturn(actorMembership);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(membershipRepository.findByTenantAndUserAndArchivedFalse(tenant, targetUser))
                .thenReturn(Optional.of(targetMembership));
        when(membershipRepository.countByTenantAndRoleAndArchivedFalse(tenant, UserRole.OWNER)).thenReturn(1L);

        UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
        req.setRole(UserRole.ADMIN);

        assertThrows(TenantAccessDeniedException.class, () ->
                service.updateMemberRole(tenantId, actorUserId, targetUserId, req));

        verify(membershipRepository, never()).save(any());
    }

    @Test
    void removeMember_blocksRemovingLastOwner() {
        UUID targetUserId = UUID.randomUUID();
        User targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setEmail("owner2@test.com");

        TenantUserMembership targetMembership = new TenantUserMembership();
        targetMembership.setTenant(tenant);
        targetMembership.setUser(targetUser);
        targetMembership.setRole(UserRole.OWNER);

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(actorUserId), any())).thenReturn(actorMembership);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(membershipRepository.findByTenantAndUserAndArchivedFalse(tenant, targetUser))
                .thenReturn(Optional.of(targetMembership));
        when(membershipRepository.countByTenantAndRoleAndArchivedFalse(tenant, UserRole.OWNER)).thenReturn(1L);

        assertThrows(TenantAccessDeniedException.class, () ->
                service.removeMember(tenantId, actorUserId, targetUserId));

        verify(membershipRepository, never()).save(any());
    }
}
