package com.roofingcrm.service.auth;

import com.roofingcrm.api.v1.auth.AuthResponse;
import com.roofingcrm.api.v1.auth.LoginRequest;
import com.roofingcrm.api.v1.auth.RegisterRequest;
import com.roofingcrm.api.v1.auth.RegisterWithInviteRequest;
import com.roofingcrm.api.v1.auth.TenantSummaryDto;
import com.roofingcrm.api.v1.team.AcceptInviteRequest;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantInvite;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.entity.AuthRefreshTokenSession;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.AuthRefreshTokenSessionRepository;
import com.roofingcrm.domain.repository.TenantInviteRepository;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.repository.TenantUserMembershipRepository;
import com.roofingcrm.domain.repository.UserRepository;
import com.roofingcrm.security.JwtService;
import com.roofingcrm.security.RefreshTokenProperties;
import com.roofingcrm.service.exception.InviteConflictException;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.pipeline.PipelineStatusAdminService;
import com.roofingcrm.service.team.TeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final int CSRF_TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantUserMembershipRepository membershipRepository;
    private final TenantInviteRepository inviteRepository;
    private final AuthRefreshTokenSessionRepository refreshTokenSessionRepository;
    private final TeamService teamService;
    private final JwtService jwtService;
    private final RefreshTokenProperties refreshTokenProperties;
    private final PipelineStatusAdminService pipelineStatusAdminService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(UserRepository userRepository,
                           TenantRepository tenantRepository,
                           TenantUserMembershipRepository membershipRepository,
                           TenantInviteRepository inviteRepository,
                           AuthRefreshTokenSessionRepository refreshTokenSessionRepository,
                           TeamService teamService,
                           JwtService jwtService,
                           RefreshTokenProperties refreshTokenProperties,
                           PipelineStatusAdminService pipelineStatusAdminService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.inviteRepository = inviteRepository;
        this.refreshTokenSessionRepository = refreshTokenSessionRepository;
        this.teamService = teamService;
        this.jwtService = jwtService;
        this.refreshTokenProperties = refreshTokenProperties;
        this.pipelineStatusAdminService = pipelineStatusAdminService;
    }

    @Override
    public AuthResponse registerOwner(RegisterRequest request) {
        // Check if user already exists
        userRepository.findByEmailIgnoreCase(request.getEmail())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("User with that email already exists");
                });

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user = userRepository.save(user);

        Tenant tenant = new Tenant();
        tenant.setName(request.getTenantName());
        // naive slug; in a real system ensure uniqueness and nicer slugging
        tenant.setSlug(request.getTenantName().toLowerCase().replace(" ", "-"));
        tenant = tenantRepository.save(tenant);
        pipelineStatusAdminService.seedDefaultsForNewTenant(tenant);

        TenantUserMembership membership = new TenantUserMembership();
        membership.setTenant(tenant);
        membership.setUser(user);
        membership.setRole(UserRole.OWNER);
        membershipRepository.save(membership);

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse registerWithInvite(RegisterWithInviteRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        TenantInvite invite = inviteRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        if (invite.getExpiresAt().isBefore(java.time.Instant.now())) {
            throw new InviteConflictException("Invite has expired.");
        }
        if (invite.getAcceptedAt() != null) {
            throw new InviteConflictException("Invite has already been accepted.");
        }
        if (!invite.getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new InviteConflictException("Invite email does not match registration email.");
        }

        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .ifPresent(existing -> {
                    throw new InviteConflictException("User with that email already exists. Please sign in to accept the invite.");
                });

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user = userRepository.save(user);

        AcceptInviteRequest acceptInviteRequest = new AcceptInviteRequest();
        acceptInviteRequest.setToken(request.getToken());
        teamService.acceptInvite(user.getId(), acceptInviteRequest);

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("User account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResourceNotFoundException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    @Override
    // Reuse detection and "expired/disabled" paths intentionally mutate session state
    // before throwing AuthSessionException; we don't want those defensive revocations
    // rolled back when we signal 401 to the controller.
    @Transactional(noRollbackFor = AuthSessionException.class)
    public AuthResponse refresh(String refreshToken, String csrfHeaderValue, String userAgent, String ipAddress) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthSessionException("Missing refresh token");
        }
        if (csrfHeaderValue == null || csrfHeaderValue.isBlank()) {
            throw new AuthSessionException("Missing refresh CSRF token");
        }

        Instant now = Instant.now();
        AuthRefreshTokenSession current = refreshTokenSessionRepository.findByTokenHash(hashToken(refreshToken))
                .orElseThrow(() -> new AuthSessionException("Invalid refresh token"));

        if (current.getRevokedAt() != null && current.getReplacedById() != null) {
            // Reuse of an already-rotated refresh token => family compromise.
            UUID familyId = current.getFamilyId();
            int revoked = revokeFamily(familyId, now);
            log.warn("Refresh token reuse detected for userId={} familyId={}; revoked {} active session(s) in family",
                    safeUserId(current), familyId, revoked);
            throw new AuthSessionException("Refresh token reuse detected");
        }

        if (!current.isUsable(now)) {
            if (current.getRevokedAt() == null) {
                current.setRevokedAt(now);
            }
            throw new AuthSessionException("Refresh token expired or revoked");
        }

        if (!constantTimeEquals(current.getCsrfTokenHash(), hashToken(csrfHeaderValue))) {
            log.warn("Refresh rejected: CSRF token mismatch for userId={}", safeUserId(current));
            throw new AuthSessionException("Invalid refresh CSRF token");
        }

        User user = current.getUser();
        if (!user.isEnabled()) {
            current.setRevokedAt(now);
            throw new AuthSessionException("User account is disabled");
        }

        String nextRefreshPlaintext = generateOpaqueToken(REFRESH_TOKEN_BYTES);
        String nextCsrfPlaintext = generateOpaqueToken(CSRF_TOKEN_BYTES);
        AuthRefreshTokenSession next = createRefreshSession(
                user,
                nextRefreshPlaintext,
                nextCsrfPlaintext,
                current.getFamilyId(),
                userAgent,
                ipAddress,
                now);
        next = refreshTokenSessionRepository.save(Objects.requireNonNull(next));

        current.setRevokedAt(now);
        current.setReplacedById(next.getId());

        AuthResponse response = buildAuthResponseWithoutRefresh(user);
        response.setRefreshToken(nextRefreshPlaintext);
        response.setCsrfToken(nextCsrfPlaintext);
        return response;
    }

    @Override
    @Transactional(noRollbackFor = AuthSessionException.class)
    public void logout(String refreshToken, String csrfHeaderValue) {
        if (refreshToken == null || refreshToken.isBlank()) {
            // Nothing to do; client is already effectively logged out.
            return;
        }
        if (csrfHeaderValue == null || csrfHeaderValue.isBlank()) {
            throw new AuthSessionException("Missing refresh CSRF token");
        }
        refreshTokenSessionRepository.findByTokenHash(hashToken(refreshToken))
                .ifPresent(session -> {
                    // Only revoke when the CSRF header matches; this prevents a third-party site
                    // from forcing a logout with the user's HttpOnly refresh cookie.
                    if (!constantTimeEquals(session.getCsrfTokenHash(), hashToken(csrfHeaderValue))) {
                        log.warn("Logout rejected: CSRF token mismatch for userId={}", safeUserId(session));
                        throw new AuthSessionException("Invalid refresh CSRF token");
                    }
                    if (session.getRevokedAt() == null) {
                        session.setRevokedAt(Instant.now());
                    }
                });
    }

    private AuthResponse buildAuthResponse(User user) {
        revokeActiveRefreshSessions(user);
        String refreshToken = generateOpaqueToken(REFRESH_TOKEN_BYTES);
        String csrfToken = generateOpaqueToken(CSRF_TOKEN_BYTES);
        refreshTokenSessionRepository.save(Objects.requireNonNull(createRefreshSession(
                user, refreshToken, csrfToken, UUID.randomUUID(), null, null, Instant.now())));
        AuthResponse response = buildAuthResponseWithoutRefresh(user);
        response.setRefreshToken(refreshToken);
        response.setCsrfToken(csrfToken);
        return response;
    }

    private AuthResponse buildAuthResponseWithoutRefresh(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail());

        List<TenantUserMembership> memberships = membershipRepository.findByUserAndArchivedFalse(user);

        List<TenantSummaryDto> tenantDtos = memberships.stream()
                .map(m -> {
                    TenantSummaryDto dto = new TenantSummaryDto();
                    dto.setTenantId(m.getTenant().getId());
                    dto.setTenantName(m.getTenant().getName());
                    dto.setTenantSlug(m.getTenant().getSlug());
                    dto.setRole(m.getRole());
                    return dto;
                })
                .toList();

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setTenants(tenantDtos);
        return response;
    }

    private void revokeActiveRefreshSessions(User user) {
        Instant now = Instant.now();
        for (AuthRefreshTokenSession session :
                refreshTokenSessionRepository.findByUserAndRevokedAtIsNullAndExpiresAtAfter(user, now)) {
            session.setRevokedAt(now);
        }
    }

    private int revokeFamily(UUID familyId, Instant now) {
        int count = 0;
        for (AuthRefreshTokenSession session : refreshTokenSessionRepository.findByFamilyIdAndRevokedAtIsNull(familyId)) {
            session.setRevokedAt(now);
            count++;
        }
        return count;
    }

    private AuthRefreshTokenSession createRefreshSession(User user,
                                                        String refreshToken,
                                                        String csrfToken,
                                                        UUID familyId,
                                                        String userAgent,
                                                        String ipAddress,
                                                        Instant now) {
        AuthRefreshTokenSession session = new AuthRefreshTokenSession();
        session.setUser(user);
        session.setTokenHash(hashToken(refreshToken));
        session.setCsrfTokenHash(hashToken(csrfToken));
        session.setFamilyId(familyId);
        session.setIssuedAt(now);
        session.setExpiresAt(now.plus(refreshTokenProperties.getExpirationDays(), ChronoUnit.DAYS));
        session.setUserAgent(truncate(userAgent, 512));
        session.setIpAddress(truncate(ipAddress, 64));
        return session;
    }

    private String generateOpaqueToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Visible for tests; SHA-256 hex digest of an opaque random token.
     */
    static String hashRefreshToken(String refreshToken) {
        return hashToken(refreshToken);
    }

    static String hashToken(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash token", ex);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }

    private static UUID safeUserId(AuthRefreshTokenSession session) {
        try {
            return session.getUser() != null ? session.getUser().getId() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
