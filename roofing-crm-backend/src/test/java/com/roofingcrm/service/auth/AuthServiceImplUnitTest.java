package com.roofingcrm.service.auth;

import com.roofingcrm.api.v1.auth.RegisterWithInviteRequest;
import com.roofingcrm.api.v1.team.AcceptInviteRequest;
import com.roofingcrm.api.v1.team.AcceptInviteResponse;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantInvite;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.TenantInviteRepository;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.repository.TenantUserMembershipRepository;
import com.roofingcrm.domain.repository.UserRepository;
import com.roofingcrm.security.JwtService;
import com.roofingcrm.service.exception.InviteConflictException;
import com.roofingcrm.service.pipeline.PipelineStatusAdminService;
import com.roofingcrm.service.team.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AuthServiceImplUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantUserMembershipRepository membershipRepository;
    @Mock
    private TenantInviteRepository inviteRepository;
    @Mock
    private TeamService teamService;
    @Mock
    private JwtService jwtService;
    @Mock
    private PipelineStatusAdminService pipelineStatusAdminService;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(
                userRepository,
                tenantRepository,
                membershipRepository,
                inviteRepository,
                teamService,
                jwtService,
                pipelineStatusAdminService
        );
    }

    @Test
    void registerWithInvite_createsUserWithoutCreatingTenant() {
        UUID token = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("Invite Tenant");
        tenant.setSlug("invite-tenant");

        TenantInvite invite = new TenantInvite();
        invite.setInviteId(UUID.randomUUID());
        invite.setTenant(tenant);
        invite.setEmail("invitee@example.com");
        invite.setRole(UserRole.SALES);
        invite.setToken(token);
        invite.setExpiresAt(Instant.now().plusSeconds(3600));

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("invitee@example.com");
        savedUser.setFullName("Invited User");
        savedUser.setEnabled(true);

        TenantUserMembership membership = new TenantUserMembership();
        membership.setTenant(tenant);
        membership.setUser(savedUser);
        membership.setRole(UserRole.SALES);

        when(inviteRepository.findByToken(token)).thenReturn(Optional.of(invite));
        when(userRepository.findByEmailIgnoreCase("invitee@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser.getId(), savedUser.getEmail())).thenReturn("jwt-token");
        when(membershipRepository.findByUserAndArchivedFalse(savedUser)).thenReturn(List.of(membership));

        AcceptInviteResponse acceptInviteResponse = new AcceptInviteResponse();
        acceptInviteResponse.setTenantId(tenant.getId());
        acceptInviteResponse.setTenantName(tenant.getName());
        acceptInviteResponse.setTenantSlug(tenant.getSlug());
        acceptInviteResponse.setRole(UserRole.SALES);
        when(teamService.acceptInvite(eq(savedUser.getId()), any(AcceptInviteRequest.class))).thenReturn(acceptInviteResponse);

        RegisterWithInviteRequest request = new RegisterWithInviteRequest();
        request.setEmail("invitee@example.com");
        request.setPassword("password123");
        request.setFullName("Invited User");
        request.setToken(token);

        var response = service.registerWithInvite(request);

        assertEquals("invitee@example.com", response.getEmail());
        assertEquals(1, response.getTenants().size());
        assertEquals("Invite Tenant", response.getTenants().get(0).getTenantName());
        verify(tenantRepository, never()).save(any());

        ArgumentCaptor<AcceptInviteRequest> acceptCaptor = ArgumentCaptor.forClass(AcceptInviteRequest.class);
        verify(teamService).acceptInvite(eq(savedUser.getId()), acceptCaptor.capture());
        assertEquals(token, acceptCaptor.getValue().getToken());
    }

    @Test
    void registerWithInvite_rejectsExpiredInvite() {
        UUID token = UUID.randomUUID();
        TenantInvite invite = new TenantInvite();
        invite.setInviteId(UUID.randomUUID());
        invite.setEmail("invitee@example.com");
        invite.setToken(token);
        invite.setExpiresAt(Instant.now().minusSeconds(60));

        when(inviteRepository.findByToken(token)).thenReturn(Optional.of(invite));

        RegisterWithInviteRequest request = new RegisterWithInviteRequest();
        request.setEmail("invitee@example.com");
        request.setPassword("password123");
        request.setFullName("Invited User");
        request.setToken(token);

        assertThrows(InviteConflictException.class, () -> service.registerWithInvite(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerWithInvite_rejectsMismatchedEmail() {
        UUID token = UUID.randomUUID();
        TenantInvite invite = new TenantInvite();
        invite.setInviteId(UUID.randomUUID());
        invite.setEmail("invitee@example.com");
        invite.setToken(token);
        invite.setExpiresAt(Instant.now().plusSeconds(3600));

        when(inviteRepository.findByToken(token)).thenReturn(Optional.of(invite));

        RegisterWithInviteRequest request = new RegisterWithInviteRequest();
        request.setEmail("other@example.com");
        request.setPassword("password123");
        request.setFullName("Invited User");
        request.setToken(token);

        assertThrows(InviteConflictException.class, () -> service.registerWithInvite(request));
        verify(userRepository, never()).save(any());
    }
}
