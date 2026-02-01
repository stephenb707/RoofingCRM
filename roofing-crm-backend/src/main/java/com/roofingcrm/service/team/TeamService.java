package com.roofingcrm.service.team;

import com.roofingcrm.api.v1.team.*;

import java.util.List;
import java.util.UUID;

public interface TeamService {

    List<TeamMemberDto> listMembers(UUID tenantId, UUID actorUserId);

    List<TenantInviteDto> listInvites(UUID tenantId, UUID actorUserId);

    TenantInviteDto createInvite(UUID tenantId, UUID actorUserId, CreateInviteRequest request);

    void revokeInvite(UUID tenantId, UUID actorUserId, UUID inviteId);

    TeamMemberDto updateMemberRole(UUID tenantId, UUID actorUserId, UUID targetUserId, UpdateMemberRoleRequest request);

    void removeMember(UUID tenantId, UUID actorUserId, UUID targetUserId);

    AcceptInviteResponse acceptInvite(UUID actorUserId, AcceptInviteRequest request);
}
