package com.roofingcrm.service.auth;

import com.roofingcrm.api.v1.auth.RegisterWithInviteRequest;
import com.roofingcrm.api.v1.team.AcceptInviteRequest;
import com.roofingcrm.api.v1.team.AcceptInviteResponse;
import com.roofingcrm.domain.entity.AuthRefreshTokenSession;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantInvite;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.TenantInviteRepository;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.repository.TenantUserMembershipRepository;
import com.roofingcrm.domain.repository.UserRepository;
import com.roofingcrm.domain.repository.AuthRefreshTokenSessionRepository;
import com.roofingcrm.security.JwtService;
import com.roofingcrm.security.RefreshTokenProperties;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
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
    private AuthRefreshTokenSessionRepository refreshTokenSessionRepository;
    @Mock
    private TeamService teamService;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenProperties refreshTokenProperties;
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
                refreshTokenSessionRepository,
                teamService,
                jwtService,
                refreshTokenProperties,
                pipelineStatusAdminService
        );
        lenient().when(refreshTokenProperties.getExpirationDays()).thenReturn(14L);
        lenient().when(refreshTokenSessionRepository.save(any())).thenAnswer(invocation -> {
            AuthRefreshTokenSession session = invocation.getArgument(0);
            if (session.getId() == null) {
                session.setId(UUID.randomUUID());
            }
            return session;
        });
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

    private static AuthRefreshTokenSession buildSession(User user, String refreshToken, String csrfToken, UUID familyId) {
        AuthRefreshTokenSession session = new AuthRefreshTokenSession();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setTokenHash(AuthServiceImpl.hashToken(refreshToken));
        session.setCsrfTokenHash(AuthServiceImpl.hashToken(csrfToken));
        session.setFamilyId(familyId != null ? familyId : UUID.randomUUID());
        session.setIssuedAt(Instant.now().minusSeconds(60));
        session.setExpiresAt(Instant.now().plusSeconds(3600));
        return session;
    }

    private static User enabledUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setFullName("Refresh User");
        user.setEnabled(true);
        return user;
    }

    @Test
    void refresh_rotatesRefreshAndCsrfTokensAndRevokesOldSession() {
        String oldRefresh = "old-refresh-token";
        String oldCsrf = "old-csrf-token";
        User user = enabledUser();
        AuthRefreshTokenSession oldSession = buildSession(user, oldRefresh, oldCsrf, null);

        when(refreshTokenSessionRepository.findByTokenHash(AuthServiceImpl.hashToken(oldRefresh)))
                .thenReturn(Optional.of(oldSession));
        when(jwtService.generateToken(user.getId(), user.getEmail())).thenReturn("new-access");
        when(membershipRepository.findByUserAndArchivedFalse(user)).thenReturn(List.of());

        var response = service.refresh(oldRefresh, oldCsrf, "UA", "127.0.0.1");

        assertEquals("new-access", response.getToken());
        assertNotNull(response.getRefreshToken());
        assertNotEquals(oldRefresh, response.getRefreshToken());
        assertNotNull(response.getCsrfToken());
        assertNotEquals(oldCsrf, response.getCsrfToken());
        assertNotNull(oldSession.getRevokedAt());
        assertNotNull(oldSession.getReplacedById());
        verify(refreshTokenSessionRepository).save(org.mockito.ArgumentMatchers.argThat(session ->
                session != null
                        && !AuthServiceImpl.hashToken(oldRefresh).equals(session.getTokenHash())
                        && session.getTokenHash().matches("[0-9a-f]{64}")
                        && session.getCsrfTokenHash() != null
                        && session.getCsrfTokenHash().matches("[0-9a-f]{64}")
        ));
    }

    @Test
    void refresh_rejectsMissingCsrfHeader() {
        assertThrows(AuthSessionException.class,
                () -> service.refresh("any-refresh", null, "UA", "127.0.0.1"));
        assertThrows(AuthSessionException.class,
                () -> service.refresh("any-refresh", "  ", "UA", "127.0.0.1"));
    }

    @Test
    void refresh_rejectsMismatchedCsrfHeader() {
        String refresh = "good-refresh";
        AuthRefreshTokenSession session = buildSession(enabledUser(), refresh, "expected-csrf", null);

        when(refreshTokenSessionRepository.findByTokenHash(AuthServiceImpl.hashToken(refresh)))
                .thenReturn(Optional.of(session));

        assertThrows(AuthSessionException.class,
                () -> service.refresh(refresh, "wrong-csrf", "UA", "127.0.0.1"));
        // Old session should NOT be revoked on a CSRF mismatch (it is still a valid session).
        assertNull(session.getRevokedAt());
    }

    @Test
    void refresh_rejectsRevokedTokenWithoutFamilyRevocation() {
        String token = "revoked-refresh-token";
        AuthRefreshTokenSession session = buildSession(enabledUser(), token, "csrf", null);
        session.setRevokedAt(Instant.now().minusSeconds(1));
        // No replacedById -> not a rotated reuse, just a stale revoked token.

        when(refreshTokenSessionRepository.findByTokenHash(AuthServiceImpl.hashToken(token)))
                .thenReturn(Optional.of(session));

        assertThrows(AuthSessionException.class, () -> service.refresh(token, "csrf", null, null));
        verify(refreshTokenSessionRepository, never()).findByFamilyIdAndRevokedAtIsNull(any());
    }

    @Test
    void refresh_reusedRotatedTokenRevokesEntireFamily() {
        UUID familyId = UUID.randomUUID();
        User user = enabledUser();

        // The old (already rotated) refresh token: revoked + replacedById set -> the reuse marker.
        String reusedRefresh = "reused-old-refresh";
        AuthRefreshTokenSession reusedOld = buildSession(user, reusedRefresh, "old-csrf", familyId);
        reusedOld.setRevokedAt(Instant.now().minusSeconds(120));
        reusedOld.setReplacedById(UUID.randomUUID());

        // The currently-active newest token in the same family.
        AuthRefreshTokenSession activeNewest = buildSession(user, "current-refresh", "current-csrf", familyId);

        when(refreshTokenSessionRepository.findByTokenHash(AuthServiceImpl.hashToken(reusedRefresh)))
                .thenReturn(Optional.of(reusedOld));
        when(refreshTokenSessionRepository.findByFamilyIdAndRevokedAtIsNull(familyId))
                .thenReturn(new ArrayList<>(List.of(activeNewest)));

        assertThrows(AuthSessionException.class,
                () -> service.refresh(reusedRefresh, "any-csrf", "UA", "127.0.0.1"));

        assertNotNull(activeNewest.getRevokedAt(),
                "newest refresh in the family must be revoked when reuse is detected");
        verify(refreshTokenSessionRepository).findByFamilyIdAndRevokedAtIsNull(familyId);
    }

    @Test
    void refresh_afterFamilyRevocation_evenNewestTokenFails() {
        UUID familyId = UUID.randomUUID();
        User user = enabledUser();

        // Newest token in a family that just got revoked due to reuse detection in another call.
        String newestRefresh = "newest-refresh";
        AuthRefreshTokenSession newest = buildSession(user, newestRefresh, "newest-csrf", familyId);
        newest.setRevokedAt(Instant.now().minusSeconds(1));
        // Crucially: no replacedById, since this was the head of the chain at time of revocation.

        when(refreshTokenSessionRepository.findByTokenHash(AuthServiceImpl.hashToken(newestRefresh)))
                .thenReturn(Optional.of(newest));

        assertThrows(AuthSessionException.class,
                () -> service.refresh(newestRefresh, "newest-csrf", "UA", "127.0.0.1"));
    }

    @Test
    void logout_validCsrf_revokesSession() {
        String refresh = "good-refresh";
        String csrf = "good-csrf";
        AuthRefreshTokenSession session = buildSession(enabledUser(), refresh, csrf, null);

        when(refreshTokenSessionRepository.findByTokenHash(AuthServiceImpl.hashToken(refresh)))
                .thenReturn(Optional.of(session));

        service.logout(refresh, csrf);

        assertNotNull(session.getRevokedAt());
    }

    @Test
    void logout_missingOrBadCsrf_rejectsAndKeepsSession() {
        String refresh = "good-refresh";
        AuthRefreshTokenSession session = buildSession(enabledUser(), refresh, "expected-csrf", null);

        when(refreshTokenSessionRepository.findByTokenHash(AuthServiceImpl.hashToken(refresh)))
                .thenReturn(Optional.of(session));

        assertThrows(AuthSessionException.class, () -> service.logout(refresh, "wrong-csrf"));
        assertNull(session.getRevokedAt());
    }

    @Test
    void logout_missingRefreshTokenIsNoop() {
        // No exception, no repository interaction.
        service.logout(null, "any-csrf");
        service.logout("", "any-csrf");
    }

    @Test
    void logout_missingCsrfButHavingRefreshThrows() {
        assertThrows(AuthSessionException.class, () -> service.logout("some-refresh", null));
    }
}
