package com.roofingcrm.service.team;

import com.roofingcrm.api.v1.team.*;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantInvite;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.TenantInviteRepository;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.repository.TenantUserMembershipRepository;
import com.roofingcrm.domain.repository.UserRepository;
import com.roofingcrm.service.exception.InviteConflictException;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.tenant.TenantAccessDeniedException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class TeamServiceImpl implements TeamService {

    private static final Set<UserRole> INVITE_ROLES = Set.of(UserRole.OWNER, UserRole.ADMIN);
    private static final Set<UserRole> MANAGE_ROLES = Set.of(UserRole.OWNER);
    private static final int INVITE_EXPIRY_DAYS = 7;

    private static final Comparator<UserRole> ROLE_ORDER = Comparator.comparingInt(TeamServiceImpl::roleOrder);

    private static int roleOrder(UserRole r) {
        return switch (r) {
            case OWNER -> 0;
            case ADMIN -> 1;
            case SALES -> 2;
            case FIELD_TECH -> 3;
        };
    }

    private final TenantAccessService tenantAccessService;
    private final TenantInviteRepository inviteRepository;
    private final TenantUserMembershipRepository membershipRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public TeamServiceImpl(TenantAccessService tenantAccessService,
                           TenantInviteRepository inviteRepository,
                           TenantUserMembershipRepository membershipRepository,
                           TenantRepository tenantRepository,
                           UserRepository userRepository) {
        this.tenantAccessService = tenantAccessService;
        this.inviteRepository = inviteRepository;
        this.membershipRepository = membershipRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamMemberDto> listMembers(UUID tenantId, UUID actorUserId) {
        tenantAccessService.loadMembershipForUserOrThrow(Objects.requireNonNull(tenantId), Objects.requireNonNull(actorUserId));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        List<TenantUserMembership> memberships = membershipRepository.findByTenantAndArchivedFalse(tenant);

        return memberships.stream()
                .sorted(Comparator.comparing(TenantUserMembership::getRole, ROLE_ORDER)
                        .thenComparing(m -> nullToEmpty(m.getUser().getFullName()))
                        .thenComparing(m -> m.getUser().getEmail()))
                .map(this::toTeamMemberDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantInviteDto> listInvites(UUID tenantId, UUID actorUserId) {
        tenantAccessService.requireAnyRole(Objects.requireNonNull(tenantId), Objects.requireNonNull(actorUserId), Objects.requireNonNull(INVITE_ROLES));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        return inviteRepository.findByTenantAndAcceptedAtIsNull(tenant).stream()
                .map(this::toTenantInviteDto)
                .toList();
    }

    @Override
    @Transactional
    public TenantInviteDto createInvite(UUID tenantId, UUID actorUserId, CreateInviteRequest request) {
        tenantAccessService.requireAnyRole(Objects.requireNonNull(tenantId), Objects.requireNonNull(actorUserId), Objects.requireNonNull(INVITE_ROLES));

        String email = normalizeEmail(request.getEmail());

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        User createdBy = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user already a member
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            membershipRepository.findByTenantAndUserAndArchivedFalse(tenant, user).ifPresent(m -> {
                throw new InviteConflictException("User is already a team member.");
            });
        });

        // Check for existing pending invite
        inviteRepository.findByTenantAndEmailIgnoreCaseAndAcceptedAtIsNull(tenant, email)
                .ifPresent(inv -> {
                    throw new InviteConflictException("A pending invite already exists for this email.");
                });

        TenantInvite invite = new TenantInvite();
        invite.setInviteId(UUID.randomUUID());
        invite.setTenant(tenant);
        invite.setEmail(email);
        invite.setRole(request.getRole());
        invite.setToken(UUID.randomUUID());
        invite.setExpiresAt(Instant.now().plusSeconds(INVITE_EXPIRY_DAYS * 24L * 3600));
        invite.setCreatedBy(createdBy);
        invite.setCreatedAt(Instant.now());

        invite = inviteRepository.save(invite);
        return toTenantInviteDto(invite);
    }

    @Override
    @Transactional
    public void revokeInvite(UUID tenantId, UUID actorUserId, UUID inviteId) {
        tenantAccessService.requireAnyRole(Objects.requireNonNull(tenantId), Objects.requireNonNull(actorUserId), Objects.requireNonNull(INVITE_ROLES));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        TenantInvite invite = inviteRepository.findById(Objects.requireNonNull(inviteId))
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        if (!invite.getTenant().getId().equals(tenant.getId())) {
            throw new ResourceNotFoundException("Invite not found");
        }
        if (invite.getAcceptedAt() != null) {
            throw new IllegalArgumentException("Cannot revoke an accepted invite.");
        }

        inviteRepository.delete(invite);
    }

    @Override
    @Transactional
    public TeamMemberDto updateMemberRole(UUID tenantId, UUID actorUserId, UUID targetUserId, UpdateMemberRoleRequest request) {
        TenantUserMembership actorMembership = tenantAccessService.requireAnyRole(Objects.requireNonNull(tenantId), Objects.requireNonNull(actorUserId), Objects.requireNonNull(MANAGE_ROLES));

        if (actorUserId.equals(targetUserId)) {
            throw new TenantAccessDeniedException("You cannot change your own role.");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        User targetUser = userRepository.findById(Objects.requireNonNull(targetUserId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TenantUserMembership targetMembership = membershipRepository.findByTenantAndUserAndArchivedFalse(tenant, targetUser)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        UserRole newRole = request.getRole();

        if (newRole == UserRole.OWNER && actorMembership.getRole() != UserRole.OWNER) {
            throw new TenantAccessDeniedException("Only owners can assign the owner role.");
        }

        if (targetMembership.getRole() == UserRole.OWNER) {
            long ownerCount = membershipRepository.countByTenantAndRoleAndArchivedFalse(tenant, UserRole.OWNER);
            if (ownerCount <= 1) {
                throw new TenantAccessDeniedException("Cannot demote the last owner.");
            }
        }

        targetMembership.setRole(newRole);
        membershipRepository.save(targetMembership);
        return toTeamMemberDto(targetMembership);
    }

    @Override
    @Transactional
    public void removeMember(UUID tenantId, UUID actorUserId, UUID targetUserId) {
        tenantAccessService.requireAnyRole(Objects.requireNonNull(tenantId), Objects.requireNonNull(actorUserId), Objects.requireNonNull(MANAGE_ROLES));

        if (actorUserId.equals(targetUserId)) {
            throw new TenantAccessDeniedException("You cannot remove yourself.");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        User targetUser = userRepository.findById(Objects.requireNonNull(targetUserId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TenantUserMembership targetMembership = membershipRepository.findByTenantAndUserAndArchivedFalse(tenant, targetUser)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (targetMembership.getRole() == UserRole.OWNER) {
            long ownerCount = membershipRepository.countByTenantAndRoleAndArchivedFalse(tenant, UserRole.OWNER);
            if (ownerCount <= 1) {
                throw new TenantAccessDeniedException("Cannot remove the last owner.");
            }
        }

        targetMembership.setArchived(true);
        targetMembership.setArchivedAt(Instant.now());
        membershipRepository.save(targetMembership);
    }

    @Override
    @Transactional
    public AcceptInviteResponse acceptInvite(UUID actorUserId, AcceptInviteRequest request) {
        TenantInvite invite = inviteRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        if (invite.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Invite has expired.");
        }
        if (invite.getAcceptedAt() != null) {
            throw new IllegalArgumentException("Invite has already been accepted.");
        }

        User actor = userRepository.findById(Objects.requireNonNull(actorUserId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!invite.getEmail().equalsIgnoreCase(actor.getEmail())) {
            throw new TenantAccessDeniedException("This invite was sent to a different email address.");
        }

        Tenant tenant = invite.getTenant();

        // If already a member, just mark invite accepted and return
        var existingMembership = membershipRepository.findByTenantAndUserAndArchivedFalse(tenant, actor);
        if (existingMembership.isPresent()) {
        invite.setAcceptedAt(Instant.now());
        invite.setAcceptedBy(actor);
        inviteRepository.save(invite);

        AcceptInviteResponse response = new AcceptInviteResponse();
        response.setTenantId(tenant.getId());
        response.setTenantName(tenant.getName());
        response.setTenantSlug(tenant.getSlug());
        response.setRole(existingMembership.get().getRole());
        return response;
        }

        // Create membership
        TenantUserMembership membership = new TenantUserMembership();
        membership.setTenant(tenant);
        membership.setUser(actor);
        membership.setRole(invite.getRole());
        membershipRepository.save(membership);

        invite.setAcceptedAt(Instant.now());
        invite.setAcceptedBy(actor);
        inviteRepository.save(invite);

        AcceptInviteResponse response = new AcceptInviteResponse();
        response.setTenantId(tenant.getId());
        response.setTenantName(tenant.getName());
        response.setTenantSlug(tenant.getSlug());
        response.setRole(invite.getRole());
        return response;
    }

    private TeamMemberDto toTeamMemberDto(TenantUserMembership m) {
        User u = m.getUser();
        TeamMemberDto dto = new TeamMemberDto();
        dto.setUserId(u.getId());
        dto.setEmail(u.getEmail());
        dto.setFullName(u.getFullName());
        dto.setRole(m.getRole());
        return dto;
    }

    private TenantInviteDto toTenantInviteDto(TenantInvite inv) {
        TenantInviteDto dto = new TenantInviteDto();
        dto.setInviteId(inv.getInviteId());
        dto.setEmail(inv.getEmail());
        dto.setRole(inv.getRole());
        dto.setToken(inv.getToken());
        dto.setExpiresAt(inv.getExpiresAt());
        dto.setAcceptedAt(inv.getAcceptedAt());
        dto.setCreatedAt(inv.getCreatedAt());
        if (inv.getCreatedBy() != null) {
            dto.setCreatedByName(inv.getCreatedBy().getFullName() != null
                    ? inv.getCreatedBy().getFullName()
                    : inv.getCreatedBy().getEmail());
        }
        return dto;
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private static String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : "";
    }
}
